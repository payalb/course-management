package com.bi.service;

import com.bi.entity.OutboxEvent;
import com.bi.entity.OutboxEvent.EventStatus;
import com.bi.events.CourseEvent;
import com.bi.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, CourseEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TOPIC = "course-events";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_WINDOW_HOURS = 24;
    
    /**
     * Poll and publish pending events every 5 seconds
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishPendingEvents() {
        log.debug("Polling for pending outbox events...");
        
        List<OutboxEvent> pendingEvents = outboxRepository
                .findTop100ByStatusOrderByCreatedAtAsc(EventStatus.PENDING);
        
        if (!pendingEvents.isEmpty()) {
            log.info("Found {} pending events to publish", pendingEvents.size());
            pendingEvents.forEach(this::publishEvent);
        }
    }
    
    /**
     * Retry failed events every 1 minute
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void retryFailedEvents() {
        log.debug("Checking for failed events to retry...");
        
        LocalDateTime retryWindow = LocalDateTime.now().minusHours(RETRY_WINDOW_HOURS);
        List<OutboxEvent> failedEvents = outboxRepository
                .findFailedEventsForRetry(MAX_RETRIES, retryWindow);
        
        if (!failedEvents.isEmpty()) {
            log.info("Found {} failed events to retry", failedEvents.size());
            failedEvents.forEach(this::publishEvent);
        }
    }
    
    /**
     * Cleanup old published events every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupOldEvents() {
        log.debug("Cleaning up old published events...");
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = outboxRepository.deleteOldPublishedEvents(threshold);
        
        if (deleted > 0) {
            log.info("Deleted {} old published events", deleted);
        }
    }
    
    @Transactional
    protected void publishEvent(OutboxEvent outboxEvent) {
        try {
            // Deserialize payload
            CourseEvent event = objectMapper.readValue(
                    outboxEvent.getPayload(), 
                    CourseEvent.class
            );
            
            // Publish to Kafka synchronously to ensure delivery before marking as published
            kafkaTemplate.send(TOPIC, event.getCourseId().toString(), event)
                    .get(); // Block and wait for result
            
            // Update status to PUBLISHED
            outboxEvent.setStatus(EventStatus.PUBLISHED);
            outboxEvent.setPublishedAt(LocalDateTime.now());
            outboxEvent.setErrorMessage(null);
            outboxRepository.save(outboxEvent);
            
            log.info("Successfully published event: {} for course: {}", 
                    outboxEvent.getEventType(), event.getCourseId());
            
        } catch (Exception e) {
            log.error("Failed to publish event {}: {}", 
                    outboxEvent.getId(), e.getMessage());
            
            // Update status to FAILED and increment retry count
            outboxEvent.setStatus(EventStatus.FAILED);
            outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
            outboxEvent.setErrorMessage(
                    e.getMessage() != null && e.getMessage().length() > 500 
                    ? e.getMessage().substring(0, 500) 
                    : e.getMessage()
            );
            outboxRepository.save(outboxEvent);
            
            // If max retries exceeded, log error for manual intervention
            if (outboxEvent.getRetryCount() >= MAX_RETRIES) {
                log.error("Event {} exceeded max retries ({}). Manual intervention required.", 
                        outboxEvent.getId(), MAX_RETRIES);
            }
        }
    }
}
