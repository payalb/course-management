package com.bi.service;

import com.bi.document.CourseDocument;
import com.bi.dto.response.CourseDTO;
import com.bi.dto.response.PageResponse;
import com.bi.events.CourseEvent;
import com.bi.events.CourseEventType;
import com.bi.repository.CourseQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseQueryService {
    private final CourseQueryRepository repository;

    @Cacheable(value = "courses", key = "#courseId")
    public CourseDTO findById(Long courseId) {
        log.debug("Finding course by id: {}", courseId);
        CourseDocument doc = repository.findByCourseId(courseId);
        return mapToDTO(doc);
    }

    @Cacheable(value = "coursePages", key = "'page_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #includeArchived")
    public PageResponse<CourseDTO> findAll(Pageable pageable, boolean includeArchived) {
        log.debug("Finding all courses with pagination, includeArchived: {}", includeArchived);
        Page<CourseDocument> page = includeArchived ? 
            repository.findAll(pageable) :
            repository.findByStatusNot("ARCHIVED", pageable);
        List<CourseDTO> content = page.getContent().stream()
                .map(this::mapToDTO)
                .toList();
        return new PageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // Default method excluding archived courses
    public PageResponse<CourseDTO> findAll(Pageable pageable) {
        return findAll(pageable, false);
    }

    @Cacheable(value = "courseSearches", 
               key = "'search_' + #keyword + '_' + #minPrice + '_' + #maxPrice + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PageResponse<CourseDTO> searchCourses(String keyword, Double minPrice, Double maxPrice, Pageable pageable) {
        log.debug("Searching courses with keyword: {}, price range: [{} - {}]", keyword, minPrice, maxPrice);
        
        Page<CourseDocument> page;
        if (keyword != null && minPrice != null && maxPrice != null) {
            page = repository.searchCoursesWithPriceRange(keyword, minPrice, maxPrice, pageable);
        } else if (minPrice != null && maxPrice != null) {
            page = repository.findByPriceBetween(minPrice, maxPrice, pageable);
        } else if (keyword != null) {
            page = repository.searchCourses(keyword, pageable);
        } else {
            page = repository.findAll(pageable);
        }

        List<CourseDTO> content = page.getContent().stream()
                .map(this::mapToDTO)
                .toList();
        return new PageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @KafkaListener(topics = "course-events", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCourseEvent(CourseEvent event) {
        log.info("Received course event: {}", event.getEventType());
        
        CourseEventType eventType = CourseEventType.valueOf(event.getEventType());
        switch (eventType) {
            case COURSE_CREATED, COURSE_UPDATED -> upsertCourse(event);
            // No more COURSE_DELETED events as we're using soft delete
            default -> log.warn("Unhandled event type: {}", eventType);
        }
    }

    @CacheEvict(value = {"courses", "coursePages", "courseSearches"}, allEntries = true)
    protected void upsertCourse(CourseEvent event) {
        CourseDocument doc = CourseDocument.builder()
                .courseId(event.getCourseId())
                .name(event.getCourseName())
                .description(event.getDescription())
                .price(event.getPrice())
                .tags(event.getTags())
                .instructorId(event.getInstructorId())
                .status(event.getStatus())
                .updatedAt(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getTimestamp()),
                    TimeZone.getDefault().toZoneId()
                ))
                .build();

        repository.save(doc);
    }

    @CacheEvict(value = {"courses", "coursePages", "courseSearches"}, allEntries = true)
    protected void deleteCourse(Long courseId) {
        CourseDocument doc = repository.findByCourseId(courseId);
        if (doc != null) {
            repository.delete(doc);
        }
    }

    private CourseDTO mapToDTO(CourseDocument doc) {
        if (doc == null) return null;	
        return CourseDTO.builder()
                .id(doc.getCourseId())
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .tags(doc.getTags())
                .instructorId(doc.getInstructorId())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}