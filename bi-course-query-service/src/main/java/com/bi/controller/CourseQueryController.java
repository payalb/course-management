package com.bi.controller;

import com.bi.dto.response.CourseDTO;
import com.bi.dto.response.PageResponse;
import com.bi.service.CourseQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses-query")
@Tag(name = "Course Queries", description = "Course read operations")
@RequiredArgsConstructor
@Slf4j
public class CourseQueryController {
    private final CourseQueryService service;

    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID")
    public ResponseEntity<CourseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    @Operation(summary = "Get all active courses with pagination")
    public ResponseEntity<PageResponse<CourseDTO>> findAll(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Include archived courses") @RequestParam(defaultValue = "false") boolean includeArchived) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.findAll(pageable, includeArchived));
    }

    @GetMapping("/search")
    @Operation(summary = "Search courses with filters")
    public ResponseEntity<PageResponse<CourseDTO>> searchCourses(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Minimum price") @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.searchCourses(keyword, minPrice, maxPrice, pageable));
    }
}