package com.bi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String[] tags;
    private Long instructorId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

