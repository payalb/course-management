package com.bi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bi.entity.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
}