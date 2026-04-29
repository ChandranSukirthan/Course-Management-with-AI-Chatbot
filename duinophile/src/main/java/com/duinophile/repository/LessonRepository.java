package com.duinophile.repository;

import com.duinophile.model.Lesson;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LessonRepository extends MongoRepository<Lesson, String> {
    List<Lesson> findByCourseId(String courseId);
}
