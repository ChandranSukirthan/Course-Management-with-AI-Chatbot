package com.duinophile.controller;

import com.duinophile.model.Course;
import com.duinophile.model.User;
import com.duinophile.service.CourseService;
import com.duinophile.service.FeedbackService;
import com.duinophile.service.LessonService;
import com.duinophile.service.UserService;
import com.duinophile.web.CurrentUser;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/courses")
public class CourseController {

    @Autowired private CourseService courseService;
    @Autowired private LessonService lessonService;
    @Autowired private FeedbackService feedbackService;
    @Autowired private UserService userService;

    // ── Guard ──────────────────────────────────────────────────────────────

    /** Returns true when the session user has STAFF or ADMIN role. */
    private boolean isStaffOrAdmin(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return false;
        return userService.getUserById(userId)
                .map(u -> "STAFF".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                .orElse(false);
    }

    // ── Create ─────────────────────────────────────────────────────────────

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        if (!isStaffOrAdmin(session)) return "redirect:/users/login";
        model.addAttribute("course", new Course());
        model.addAttribute("view", "create-course");
        return "layout";
    }

    @PostMapping("/create")
    public String createCourse(@Valid @ModelAttribute("course") Course course,
                               BindingResult result, HttpSession session, Model model) {
        if (!isStaffOrAdmin(session)) return "redirect:/users/login";

        if (!result.hasFieldErrors("title")) {
            courseService.findByTitleIgnoreCase(course.getTitle()).ifPresent(existing ->
                    result.rejectValue("title", "error.course", "A course with this exact title already exists."));
        }
        if (result.hasErrors()) {
            model.addAttribute("view", "create-course");
            return "layout";
        }
        courseService.createCourse(course);
        return "redirect:/courses/list";
    }

    // ── List / Search ──────────────────────────────────────────────────────

    @GetMapping("/list")
    public String listCourses(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        List<Course> allCourses = courseService.getAllCourses();
        model.addAttribute("allCourses", allCourses);
        populateUserCourseModel(userId, allCourses, model);
        model.addAttribute("view", "courses-list");
        return "layout";
    }

    @GetMapping("/search")
    public String searchCourses(@RequestParam String query, HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        List<Course> results = courseService.searchCourses(query);
        model.addAttribute("allCourses", results);
        populateUserCourseModel(userId, results, model);
        model.addAttribute("view", "courses-list");
        return "layout";
    }

    // ── My Courses ─────────────────────────────────────────────────────────

    @GetMapping("/my")
    public String myCourses(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return "redirect:/users/login";

        userService.getUserById(userId).ifPresent(user -> {
            List<String> enrolledIds = user.getEnrolledCourseIds();
            List<Course> enrolled = courseService.getAllCourses().stream()
                    .filter(c -> enrolledIds != null && enrolledIds.contains(c.getId()))
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("enrolledCourses", enrolled);
            model.addAttribute("progressMap", courseService.calculateProgressMap(user, enrolled));
        });

        if (!model.containsAttribute("enrolledCourses")) {
            model.addAttribute("enrolledCourses", new ArrayList<Course>());
            model.addAttribute("progressMap", new HashMap<String, Integer>());
        }
        model.addAttribute("view", "my-courses");
        return "layout";
    }

    // ── Enroll ─────────────────────────────────────────────────────────────

    @PostMapping("/enroll/{id}")
    public String enrollInCourse(@PathVariable String id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            userService.getUserById(userId).ifPresent(user ->
                    courseService.getCourseById(id).ifPresent(course -> {
                        if (user.getPoints() >= course.getMinimumPointsRequired()) {
                            userService.enrollInCourse(userId, id);
                        }
                    })
            );
        }
        return "redirect:/courses/list";
    }

    // ── View ───────────────────────────────────────────────────────────────

    @GetMapping("/view/{id}")
    public String viewCourse(@PathVariable String id, HttpSession session, Model model,
                             @ModelAttribute("currentUser") CurrentUser currentUser) {
        String userId = (String) session.getAttribute("userId");
        courseService.getCourseById(id).ifPresent(course -> {
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessonService.getAllByCourseId(id));
            model.addAttribute("feedbacks", feedbackService.getFeedbackByCourseId(id, currentUser));

            if (userId != null) {
                userService.getUserById(userId).ifPresent(user -> {
                    boolean isEnrolled = user.getEnrolledCourseIds() != null
                            && user.getEnrolledCourseIds().contains(id);
                    model.addAttribute("isEnrolled", isEnrolled);
                    model.addAttribute("completedLessonIds",
                            user.getCompletedLessonIds() != null ? user.getCompletedLessonIds() : new ArrayList<String>());
                });
            } else {
                model.addAttribute("isEnrolled", false);
                model.addAttribute("completedLessonIds", new ArrayList<String>());
            }
        });
        model.addAttribute("view", "course-details");
        return "layout";
    }

    // ── Edit / Update / Delete ─────────────────────────────────────────────

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, HttpSession session, Model model) {
        if (!isStaffOrAdmin(session)) return "redirect:/users/login";
        courseService.getCourseById(id).ifPresent(course -> model.addAttribute("course", course));
        model.addAttribute("view", "edit-course");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updateCourse(@PathVariable String id,
                               @Valid @ModelAttribute("course") Course course,
                               BindingResult result, HttpSession session, Model model) {
        if (!isStaffOrAdmin(session)) return "redirect:/users/login";

        if (!result.hasFieldErrors("title")) {
            courseService.findByTitleIgnoreCase(course.getTitle()).ifPresent(existing -> {
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
    public String deleteCourse(@PathVariable String id, HttpSession session) {
        if (!isStaffOrAdmin(session)) return "redirect:/users/login";
        courseService.deleteCourse(id);
        return "redirect:/courses/list";
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Populates user-specific course model attributes (enrolled IDs, points, progress). */
    private void populateUserCourseModel(String userId, List<Course> courses, Model model) {
        if (userId != null) {
            userService.getUserById(userId).ifPresent(user -> {
                model.addAttribute("enrolledIds", user.getEnrolledCourseIds());
                model.addAttribute("userPoints", user.getPoints());
                model.addAttribute("progressMap", courseService.calculateProgressMap(user, courses));
            });
        } else {
            model.addAttribute("enrolledIds", new ArrayList<String>());
            model.addAttribute("userPoints", 0);
            model.addAttribute("progressMap", new HashMap<String, Integer>());
        }
    }
}
