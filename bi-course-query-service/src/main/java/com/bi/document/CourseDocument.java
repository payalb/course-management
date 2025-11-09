package com.bi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDocument {
    @Id
    private String id;
    
    private Long courseId;

    @Indexed
    private String name;

    private String description;
    private Double price;
    private String[] tags;
    private Long instructorId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}