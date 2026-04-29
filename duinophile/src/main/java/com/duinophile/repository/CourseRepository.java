package com.duinophile.repository;

import com.duinophile.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findByTitleContainingIgnoreCase(String title);
    java.util.Optional<Course> findByTitleIgnoreCase(String title);
}
