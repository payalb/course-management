package com.bi.controller;

import com.bi.dto.command.CreateCourseCommand;
import com.bi.dto.command.UpdateCourseCommand;
import com.bi.service.CourseCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@Tag(name = "Course Commands", description = "Course write operations")
@RequiredArgsConstructor
@Slf4j
public class CourseCommandController {
    private final CourseCommandService service;

    @PostMapping
    @Operation(summary = "Create a new course")
    public ResponseEntity<Long> createCourse(@Valid @RequestBody CreateCourseCommand command) {
        Long courseId = service.createCourse(command);  
        return ResponseEntity.status(HttpStatus.CREATED).body(courseId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing course")
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseCommand command) {
        service.updateCourse(id, command);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a course by setting status to ARCHIVED")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        service.deleteCourse(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore an archived course by setting status to DRAFT")
    public ResponseEntity<Void> restoreCourse(@PathVariable Long id) {
        service.restoreCourse(id);
        return ResponseEntity.ok().build();
    }
}