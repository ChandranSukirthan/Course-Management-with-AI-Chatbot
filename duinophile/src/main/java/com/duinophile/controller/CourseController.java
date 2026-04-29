package com.duinophile.controller;

import com.duinophile.model.Course;
import com.duinophile.service.CourseService;
import com.duinophile.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private com.duinophile.repository.CourseRepository courseRepository;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private com.duinophile.service.FeedbackService feedbackService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("view", "create-course");
        return "layout";
    }

    @PostMapping("/create")
    public String createCourse(@Valid @ModelAttribute("course") Course course, BindingResult result, Model model) {
        if (!result.hasFieldErrors("title")) {
            courseRepository.findByTitleIgnoreCase(course.getTitle()).ifPresent(existing -> {
                result.rejectValue("title", "error.course", "A course with this exact title already exists.");
            });
        }
        if (result.hasErrors()) {
            model.addAttribute("view", "create-course");
            return "layout";
        }
        courseService.createCourse(course);
        return "redirect:/courses/list";
    }

    @Autowired
    private com.duinophile.service.UserService userService;

    @GetMapping("/list")
    public String listCourses(jakarta.servlet.http.HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        java.util.List<Course> allCourses = courseService.getAllCourses();
        model.addAttribute("allCourses", allCourses);

        if (userId != null) {
            userService.getUserById(userId).ifPresent(user -> {
                model.addAttribute("enrolledIds", user.getEnrolledCourseIds());
                model.addAttribute("userPoints", user.getPoints());
                
                // Calculate progress for each allCourse if enrolled
                java.util.Map<String, Integer> progressMap = new java.util.HashMap<>();
                java.util.List<String> enrolledIds = user.getEnrolledCourseIds();
                java.util.List<String> completedIds = user.getCompletedLessonIds();
                
                for (Course c : allCourses) {
                    if (enrolledIds != null && enrolledIds.contains(c.getId())) {
                        java.util.List<com.duinophile.model.Lesson> lessons = lessonService.getAllByCourseId(c.getId());
                        if (lessons.isEmpty()) {
                            progressMap.put(c.getId(), 0);
                        } else {
                            long completedCount = lessons.stream()
                                    .filter(l -> completedIds != null && completedIds.contains(l.getId()))
                                    .count();
                            progressMap.put(c.getId(), (int) ((completedCount * 100) / lessons.size()));
                        }
                    }
                }
                model.addAttribute("progressMap", progressMap);
            });
        } else {
            model.addAttribute("enrolledIds", new java.util.ArrayList<String>());
            model.addAttribute("userPoints", 0);
            model.addAttribute("progressMap", new java.util.HashMap<String, Integer>());
        }

        model.addAttribute("view", "courses-list");
        return "layout";
    }

    @GetMapping("/my")
    public String myCourses(jakarta.servlet.http.HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return "redirect:/users/login";

        userService.getUserById(userId).ifPresent(user -> {
            java.util.List<String> enrolledIds = user.getEnrolledCourseIds();
            java.util.List<Course> enrolled = courseService.getAllCourses().stream()
                    .filter(c -> enrolledIds != null && enrolledIds.contains(c.getId()))
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("enrolledCourses", enrolled);

            // Calculate progress for each
            java.util.Map<String, Integer> progressMap = new java.util.HashMap<>();
            java.util.List<String> completedIds = user.getCompletedLessonIds();
            for (Course c : enrolled) {
                java.util.List<com.duinophile.model.Lesson> lessons = lessonService.getAllByCourseId(c.getId());
                if (lessons.isEmpty()) {
                    progressMap.put(c.getId(), 0);
                } else {
                    long completedCount = lessons.stream()
                            .filter(l -> completedIds != null && completedIds.contains(l.getId()))
                            .count();
                    progressMap.put(c.getId(), (int) ((completedCount * 100) / lessons.size()));
                }
            }
            model.addAttribute("progressMap", progressMap);
        });

        if (!model.containsAttribute("enrolledCourses")) {
            model.addAttribute("enrolledCourses", new java.util.ArrayList<Course>());
            model.addAttribute("progressMap", new java.util.HashMap<String, Integer>());
        }

        model.addAttribute("view", "my-courses");
        return "layout";
    }

    @PostMapping("/enroll/{id}")
    public String enrollInCourse(@PathVariable String id, jakarta.servlet.http.HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            userService.getUserById(userId).ifPresent(user -> {
                courseService.getCourseById(id).ifPresent(course -> {
                    if (user.getPoints() >= course.getMinimumPointsRequired()) {
                        userService.enrollInCourse(userId, id);
                    }
                });
            });
        }
        return "redirect:/courses/list";
    }

    @GetMapping("/view/{id}")
    public String viewCourse(@PathVariable String id, jakarta.servlet.http.HttpSession session, Model model, @ModelAttribute("currentUser") com.duinophile.web.CurrentUser currentUser) {
        String userId = (String) session.getAttribute("userId");
        courseService.getCourseById(id).ifPresent(course -> {
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessonService.getAllByCourseId(id));
            model.addAttribute("feedbacks", feedbackService.getFeedbackByCourseId(id, currentUser));

            if (userId != null) {
                userService.getUserById(userId).ifPresent(user -> {
                    boolean isEnrolled = user.getEnrolledCourseIds() != null && user.getEnrolledCourseIds().contains(id);
                    model.addAttribute("isEnrolled", isEnrolled);
                    model.addAttribute("completedLessonIds", user.getCompletedLessonIds() != null ? user.getCompletedLessonIds() : new java.util.ArrayList<String>());
                });
            } else {
                model.addAttribute("isEnrolled", false);
                model.addAttribute("completedLessonIds", new java.util.ArrayList<String>());
            }
        });
        model.addAttribute("view", "course-details");
        return "layout";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        courseService.getCourseById(id).ifPresent(course -> model.addAttribute("course", course));
        model.addAttribute("view", "edit-course");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updateCourse(@PathVariable String id, @Valid @ModelAttribute("course") Course course,
            BindingResult result, Model model) {
        if (!result.hasFieldErrors("title")) {
            courseRepository.findByTitleIgnoreCase(course.getTitle()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    result.rejectValue("title", "error.course", "Another course with this title already exists.");
                }
            });
        }
        if (result.hasErrors()) {
            model.addAttribute("view", "edit-course");
            return "layout";
        }
        courseService.updateCourse(id, course);
        return "redirect:/courses/view/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return "redirect:/courses/list";
    }

    @GetMapping("/search")
    public String searchCourses(@RequestParam String query, Model model, jakarta.servlet.http.HttpSession session) {
        java.util.List<Course> allCourses = courseService.searchCourses(query);
        model.addAttribute("allCourses", allCourses);
        
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            userService.getUserById(userId).ifPresent(user -> {
                model.addAttribute("enrolledIds", user.getEnrolledCourseIds() != null ? user.getEnrolledCourseIds() : new java.util.ArrayList<String>());
                model.addAttribute("userPoints", user.getPoints());
                model.addAttribute("progressMap", new java.util.HashMap<String, Integer>()); 
            });
        } else {
            model.addAttribute("enrolledIds", new java.util.ArrayList<String>());
            model.addAttribute("userPoints", 0);
            model.addAttribute("progressMap", new java.util.HashMap<String, Integer>());
        }

        model.addAttribute("view", "courses-list");
        return "layout";
    }
}
