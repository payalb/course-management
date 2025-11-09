package com.bi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_status_created", columnList = "status, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String eventType;
    
    @Column(nullable = false)
    private Long aggregateId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime publishedAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(length = 500)
    private String errorMessage;
    
    public enum EventStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
