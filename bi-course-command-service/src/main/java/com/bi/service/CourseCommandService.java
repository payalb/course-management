package com.bi.service;

import com.bi.dto.command.CreateCourseCommand;
import com.bi.dto.command.UpdateCourseCommand;
import com.bi.entity.Course;
import com.bi.entity.Course.CourseStatus;
import com.bi.entity.OutboxEvent;
import com.bi.entity.OutboxEvent.EventStatus;
import com.bi.events.CourseEvent;
import com.bi.events.CourseEventType;
import com.bi.exception.ResourceNotFoundException;
import com.bi.repository.CourseRepository;
import com.bi.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseCommandService {
    private final CourseRepository repository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    private static final String TOPIC = "course-events";

    @Transactional
    public Long createCourse(CreateCourseCommand command) {
        log.info("Creating new course: {}", command);
        
        Course course = Course.builder()
                .name(command.getName())
                .description(command.getDescription())
                .price(command.getPrice())
                .tags(command.getTags())
                .instructorId(command.getInstructorId())
                .status(CourseStatus.DRAFT)
                .build();

        Course saved = repository.save(course);
        
        publishEvent(CourseEventType.COURSE_CREATED, saved);
        
        return saved.getId();
    }

    @Transactional
    public void updateCourse(Long id, UpdateCourseCommand command) {
        log.info("Updating course: {}", id);
        
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (command.getName() != null) {
            course.setName(command.getName());
        }
        if (command.getDescription() != null) {
            course.setDescription(command.getDescription());
        }
        if (command.getPrice() != null) {
            course.setPrice(command.getPrice());
        }
        if (command.getTags() != null) {
            course.setTags(command.getTags());
        }
        if (command.getStatus() != null) {
            course.setStatus(CourseStatus.valueOf(command.getStatus()));
        }

        Course updated = repository.save(course);
        publishEvent(CourseEventType.COURSE_UPDATED, updated);
    }

    @Transactional
    public void deleteCourse(Long id) {
        log.info("Soft deleting course: {}", id);
        
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        course.setStatus(CourseStatus.ARCHIVED);
        Course updated = repository.save(course);
        publishEvent(CourseEventType.COURSE_UPDATED, updated);
    }

    @Transactional
    public void restoreCourse(Long id) {
        log.info("Restoring archived course: {}", id);
        
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
                
        if (course.getStatus() != CourseStatus.ARCHIVED) {
            throw new IllegalStateException("Can only restore archived courses. Current status: " + course.getStatus());
        }

        course.setStatus(CourseStatus.DRAFT);  // Restore to DRAFT state for review
        Course updated = repository.save(course);
        publishEvent(CourseEventType.COURSE_UPDATED, updated);
    }

    private void publishEvent(CourseEventType eventType, Course course) {
        CourseEvent event = new CourseEvent(
            eventType.name(),
            course.getId(),
            course.getName(),
            course.getDescription(),
            course.getPrice(),
            course.getTags(),
            course.getInstructorId(),
            course.getStatus().name(),
            System.currentTimeMillis()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(eventType.name())
                    .aggregateId(course.getId())
                    .payload(payload)
                    .status(EventStatus.PENDING)
                    .build();
            
            outboxRepository.save(outboxEvent);
            log.debug("Saved event to outbox: {}", eventType);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload: {}", e.getMessage());
            throw new RuntimeException("Failed to save event to outbox", e);
        }
    }
}