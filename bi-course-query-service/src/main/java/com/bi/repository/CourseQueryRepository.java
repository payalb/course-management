package com.bi.repository;

import com.bi.document.CourseDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseQueryRepository extends MongoRepository<CourseDocument, String> {
    
    CourseDocument findByCourseId(Long courseId);
    
    Page<CourseDocument> findByStatusNot(String status, Pageable pageable);
    
    @Query("{ $and: [ " +
           "{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'description': { $regex: ?0, $options: 'i' } }, " +
           "{ 'tags': { $in: [?0] } } " +
           "] }, " +
           "{ 'status': { $ne: 'ARCHIVED' } } " +
           "] }")
    Page<CourseDocument> searchCourses(String keyword, Pageable pageable);
    
    @Query("{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'description': { $regex: ?0, $options: 'i' } }, " +
           "{ 'tags': { $in: [?0] } } " +
           "] }")
    Page<CourseDocument> searchAllCourses(String keyword, Pageable pageable);
    
    Page<CourseDocument> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);
    
    @Query("{ $and: [ " +
           "{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'description': { $regex: ?0, $options: 'i' } }, " +
           "{ 'tags': { $in: [?0] } } " +
           "] }, " +
           "{ 'price': { $gte: ?1, $lte: ?2 } } " +
           "] }")
    Page<CourseDocument> searchCoursesWithPriceRange(String keyword, Double minPrice, Double maxPrice, Pageable pageable);
}