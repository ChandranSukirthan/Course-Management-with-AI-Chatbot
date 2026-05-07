package com.duinophile.service;

import com.duinophile.model.Lesson;
import com.duinophile.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LessonService {

    @Autowired
    private LessonRepository lessonRepository;

    public Lesson createLesson(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public List<Lesson> getAllByCourseId(String courseId) {
        return lessonRepository.findByCourseId(courseId);
    }

    public Optional<Lesson> getById(String id) {
        return lessonRepository.findById(id);
    }

    public Lesson updateLesson(String id, Lesson lessonDetails) {
        Lesson lesson = lessonRepository.findById(id).orElseThrow(() -> new RuntimeException("Lesson not found"));
        lesson.setTitle(lessonDetails.getTitle());
        lesson.setContent(lessonDetails.getContent());
        lesson.setQuiz(lessonDetails.getQuiz());
        lesson.setMaterialUrl(lessonDetails.getMaterialUrl());
        lesson.setMaterialName(lessonDetails.getMaterialName());
        return lessonRepository.save(lesson);
    }

    public void deleteLesson(String id) {
        lessonRepository.deleteById(id);
    }
}
