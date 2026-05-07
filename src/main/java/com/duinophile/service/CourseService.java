package com.duinophile.service;

import com.duinophile.model.Course;
import com.duinophile.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonService lessonService;

    // ── CRUD ─────────────────────────────────────────────────────────────

    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(String id) {
        return courseRepository.findById(id);
    }

    /**
     * Updates all editable fields on a course.
     * Previously only title, description, and completed were synced — level,
     * minimumPointsRequired were silently dropped on every edit.
     */
    public Course updateCourse(String id, Course courseDetails) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setTitle(courseDetails.getTitle());
        course.setDescription(courseDetails.getDescription());
        course.setLevel(courseDetails.getLevel());
        course.setMinimumPointsRequired(courseDetails.getMinimumPointsRequired());
        course.setCompleted(courseDetails.isCompleted());
        return courseRepository.save(course);
    }

    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }

    public List<Course> searchCourses(String query) {
        return courseRepository.findByTitleContainingIgnoreCase(query);
    }

    /** Delegate title uniqueness check — keeps controllers away from the repository layer */
    public Optional<Course> findByTitleIgnoreCase(String title) {
        return courseRepository.findByTitleIgnoreCase(title);
    }

    // ── Progress Calculation ─────────────────────────────────────────────

    public java.util.Map<String, Integer> calculateProgressMap(com.duinophile.model.User user, List<Course> courses) {
        java.util.Map<String, Integer> progressMap = new java.util.HashMap<>();
        if (user == null || courses == null) return progressMap;

        java.util.List<String> enrolledIds = user.getEnrolledCourseIds();
        java.util.List<String> completedIds = user.getCompletedLessonIds();

        for (Course c : courses) {
            if (enrolledIds != null && enrolledIds.contains(c.getId())) {
                java.util.List<com.duinophile.model.Lesson> lessons = lessonService.getAllByCourseId(c.getId());
                if (lessons == null || lessons.isEmpty()) {
                    progressMap.put(c.getId(), 0);
                } else {
                    long completedCount = lessons.stream()
                            .filter(l -> completedIds != null && completedIds.contains(l.getId()))
                            .count();
                    progressMap.put(c.getId(), (int) ((completedCount * 100) / lessons.size()));
                }
            }
        }
        return progressMap;
    }
}
