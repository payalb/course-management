package com.bi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(nullable = false)
    private Long instructorId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @CreationTimestamp
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt= LocalDateTime.now();

    @UpdateTimestamp
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt= LocalDateTime.now();

    public enum CourseStatus {
        /**
         * Initial state when course is created
         */
        DRAFT,
        
        /**
         * Course is available for enrollment
         */
        PUBLISHED,
        
        /**
         * Course is soft deleted and not available for new enrollments
         */
        ARCHIVED
    }
}