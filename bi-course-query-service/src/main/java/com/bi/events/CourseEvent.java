package com.bi.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseEvent {
    private String eventType;
    private Long courseId;
    private String courseName;
    private String description;
    private Double price;
    private String[] tags;
    private Long instructorId;
    private String status;
    private Long timestamp;
}

