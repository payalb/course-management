package com.bi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CourseCommandServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseCommandServiceApplication.class, args);
    }
}