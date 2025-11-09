package com.bi.repository;

import com.bi.entity.OutboxEvent;
import com.bi.entity.OutboxEvent.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    /**
     * Find pending events ordered by creation time (oldest first)
     * Limit to prevent overloading the publisher
     */
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(EventStatus status);
    
    /**
     * Find failed events that are eligible for retry
     * (retry count less than max and created within retry window)
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'FAILED' " +
           "AND o.retryCount < :maxRetries " +
           "AND o.createdAt > :retryWindow " +
           "ORDER BY o.createdAt ASC")
    List<OutboxEvent> findFailedEventsForRetry(
        @Param("maxRetries") int maxRetries,
        @Param("retryWindow") LocalDateTime retryWindow
    );
    
    /**
     * Delete old published events (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :threshold")
    int deleteOldPublishedEvents(@Param("threshold") LocalDateTime threshold);
}
