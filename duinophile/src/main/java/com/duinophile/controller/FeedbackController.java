package com.duinophile.controller;

import com.duinophile.model.Feedback;
import com.duinophile.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.duinophile.web.CurrentUser;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private com.duinophile.service.CourseService courseService;

    @GetMapping("/course/{courseId}")
    public String viewFeedback(@PathVariable String courseId, Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Feedback newFeedback = new Feedback();
        newFeedback.setCourseId(courseId);
        model.addAttribute("feedback", newFeedback);
        model.addAttribute("view", "feedback-list");
        return "layout";
    }

    @GetMapping("/manage")
    public String manageFeedback(Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        if (currentUser == null || (!"ADMIN".equals(currentUser.role()) && !"STAFF".equals(currentUser.role()))) {
            return "redirect:/courses/list";
        }
        java.util.List<Feedback> feedbacks = feedbackService.getAllFeedbacks();
        java.util.Map<String, String> courseNames = new java.util.HashMap<>();
        for (Feedback fb : feedbacks) {
            if (!courseNames.containsKey(fb.getCourseId())) {
                courseService.getCourseById(fb.getCourseId())
                             .ifPresent(c -> courseNames.put(c.getId(), c.getTitle()));
            }
        }
        
        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("courseNames", courseNames);
        model.addAttribute("view", "manage-feedback");
        return "layout";
    }

    @PostMapping("/create")
    public String createFeedback(@jakarta.validation.Valid @ModelAttribute Feedback feedback, org.springframework.validation.BindingResult result, @ModelAttribute("currentUser") CurrentUser currentUser, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs, @RequestHeader(value = "Referer", required = false) String referer) {
        String target = referer != null ? "redirect:" + referer : "redirect:/feedback/course/" + feedback.getCourseId();

        if (result.hasErrors()) {
            redirectAttrs.addFlashAttribute("error", "Feedback must be between 10 and 1000 characters, and rating must be 1-5.");
            return target;
        }

        if (currentUser != null) {
            java.util.List<Feedback> allFeedback = feedbackService.getFeedbackByCourseId(feedback.getCourseId(), currentUser);
            java.util.Optional<Feedback> lastFeedback = allFeedback.stream()
                .filter(f -> currentUser.id().equals(f.getUserId()))
                .max(java.util.Comparator.comparing(Feedback::getCreatedAt));
                
            if (lastFeedback.isPresent()) {
                long minutesSince = java.time.Duration.between(lastFeedback.get().getCreatedAt(), java.time.LocalDateTime.now()).toMinutes();
                if (minutesSince < 60) {
                    redirectAttrs.addFlashAttribute("error", "Spam protection: You can only submit feedback for a course once per hour.");
                    return target;
                }
            }

            feedback.setUserId(currentUser.id());
            feedback.setUsername(currentUser.username());
        } else {
            redirectAttrs.addFlashAttribute("error", "You must be logged in to submit feedback.");
            return "redirect:/login";
        }
        
        feedbackService.createFeedback(feedback);
        redirectAttrs.addFlashAttribute("success", "Review submitted successfully! It is pending admin approval.");
        return target;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Feedback feedback = feedbackService.getFeedbackById(id).orElse(null);
        if (feedback == null || currentUser == null || !currentUser.id().equals(feedback.getUserId())) {
            return "redirect:/courses/list";
        }
        model.addAttribute("feedback", feedback);
        model.addAttribute("view", "edit-feedback");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updateFeedback(@PathVariable String id, @ModelAttribute Feedback feedback, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Feedback existing = feedbackService.getFeedbackById(id).orElse(null);
        if (existing != null && currentUser != null && currentUser.id().equals(existing.getUserId())) {
            feedbackService.updateFeedback(id, feedback);
            return "redirect:/courses/view/" + existing.getCourseId();
        }
        return "redirect:/courses/list";
    }

    @PostMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser, @RequestParam(value = "redirect", required = false) String redirectUrl) {
        Feedback feedback = feedbackService.getFeedbackById(id).orElse(null);
        if (feedback != null && currentUser != null && (currentUser.id().equals(feedback.getUserId()) || "ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role()))) {
            feedbackService.deleteFeedback(id);
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/courses/view/" + feedback.getCourseId();
        }
        return "redirect:/courses/list";
    }

    @PostMapping("/approve/{id}")
    public String approveFeedback(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser, @RequestParam(value = "redirect", required = false) String redirectUrl) {
        Feedback feedback = feedbackService.getFeedbackById(id).orElse(null);
        if (feedback != null && currentUser != null && ("ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role()))) {
            feedbackService.approveFeedback(id);
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/courses/view/" + feedback.getCourseId();
        }
        return "redirect:/courses/list";
    }
}
