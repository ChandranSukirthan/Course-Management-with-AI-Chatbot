package com.duinophile.controller;

import com.duinophile.model.Lesson;
import com.duinophile.service.LessonService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

@Controller
@RequestMapping("/lessons")
public class LessonController {

    @Autowired
    private LessonService lessonService;

    @Autowired
    private com.duinophile.service.UserService userService;

    @GetMapping("/create/{courseId}")
    public String showCreateForm(@PathVariable String courseId, Model model) {
        Lesson lesson = new Lesson();
        lesson.setCourseId(courseId);
        model.addAttribute("lesson", lesson);
        model.addAttribute("view", "create-lesson");
        return "layout";
    }

    @PostMapping("/create")
    public String createLesson(@Valid @ModelAttribute("lesson") Lesson lesson,
                               BindingResult result,
                               @RequestParam(value = "materialFile", required = false) MultipartFile materialFile,
                               Model model) {

        validateQuizzes(lesson, result);
        validateFile(materialFile, result);

        if (result.hasErrors()) {
            model.addAttribute("view", "create-lesson");
            return "layout";
        }
        handleFileUpload(lesson, materialFile);
        lessonService.createLesson(lesson);
        return "redirect:/courses/view/" + lesson.getCourseId();
    }

    @GetMapping("/view/{id}")
    public String viewLesson(@PathVariable String id, HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        lessonService.getById(id).ifPresent(lesson -> {
            model.addAttribute("lesson", lesson);
            if (userId != null) {
                userService.getUserById(userId).ifPresent(user -> {
                    boolean isEnrolled = user.getEnrolledCourseIds() != null && user.getEnrolledCourseIds().contains(lesson.getCourseId());
                    model.addAttribute("isEnrolled", isEnrolled);
                    model.addAttribute("completedLessonIds", user.getCompletedLessonIds());
                });
            } else {
                model.addAttribute("isEnrolled", false);
                model.addAttribute("completedLessonIds", new java.util.ArrayList<String>());
            }
        });
        model.addAttribute("view", "lesson-view");
        return "layout";
    }

    @GetMapping("/quiz/{id}")
    public String viewQuiz(@PathVariable String id, HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        lessonService.getById(id).ifPresent(lesson -> {
            model.addAttribute("lesson", lesson);
            if (userId != null) {
                userService.getUserById(userId).ifPresent(user -> {
                    boolean isEnrolled = user.getEnrolledCourseIds() != null && user.getEnrolledCourseIds().contains(lesson.getCourseId());
                    model.addAttribute("isEnrolled", isEnrolled);
                });
            } else {
                model.addAttribute("isEnrolled", false);
            }
        });
        model.addAttribute("view", "lesson-quiz");
        return "layout";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        lessonService.getById(id).ifPresent(lesson -> model.addAttribute("lesson", lesson));
        model.addAttribute("view", "edit-lesson");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updateLesson(@PathVariable String id, @Valid @ModelAttribute("lesson") Lesson lesson,
                               BindingResult result,
                               @RequestParam(value = "materialFile", required = false) MultipartFile materialFile,
                               Model model) {

        validateQuizzes(lesson, result);
        validateFile(materialFile, result);

        if (result.hasErrors()) {
            model.addAttribute("view", "edit-lesson");
            return "layout";
        }

        lessonService.getById(id).ifPresent(existingLesson -> {
            if (materialFile == null || materialFile.isEmpty()) {
                // Preserve existing material if a new file isn't uploaded
                lesson.setMaterialUrl(existingLesson.getMaterialUrl());
                lesson.setMaterialName(existingLesson.getMaterialName());
            } else {
                // Delete old file from disk before uploading new one
                deletePhysicalFile(existingLesson.getMaterialUrl());
                handleFileUpload(lesson, materialFile);
            }
        });

        lessonService.updateLesson(id, lesson);
        return "redirect:/lessons/view/" + id;
    }

    private void validateQuizzes(Lesson lesson, BindingResult result) {
        if (lesson.getQuiz() != null) {
            for (int i = 0; i < lesson.getQuiz().size(); i++) {
                Lesson.QuizQuestion q = lesson.getQuiz().get(i);
                if (q.getOptions() != null) {
                    q.getOptions().removeIf(String::isBlank);
                    for (String opt : q.getOptions()) {
                        if (opt.length() > 200) {
                            result.rejectValue("quiz", "error.lesson", "Quiz Question " + (i+1) + " contains an option that exceeds 200 characters.");
                            break; // Avoid spamming multiple errors for the same question
                        }
                    }
                }
                if (q.getOptions() == null || q.getOptions().size() < 2) {
                    result.rejectValue("quiz", "error.lesson", "Quiz Question " + (i+1) + " must have at least 2 non-blank options.");
                }
            }
        }
    }

    private void validateFile(MultipartFile file, BindingResult result) {
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > 5242880) { // 5MB limit
                result.rejectValue("materialName", "error.lesson", "File size must be strictly under 5MB.");
            }
            if (!"application/pdf".equals(file.getContentType())) {
                result.rejectValue("materialName", "error.lesson", "Lesson material must be a PDF file.");
            }
        }
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(org.springframework.web.multipart.MaxUploadSizeExceededException exc, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("error", "The uploaded file exceeds the absolute 5MB server limit. Please upload a smaller PDF.");
        return "redirect:/courses/list";
    }

    private void handleFileUpload(Lesson lesson, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                String uploadsDir = "./uploads/";
                Path uploadPath = Paths.get(uploadsDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath);

                lesson.setMaterialUrl("/uploads/" + fileName);
                lesson.setMaterialName(file.getOriginalFilename());
            } catch (IOException e) {
                // Log and handle error
                System.err.println("Failed to upload file: " + e.getMessage());
            }
        }
    }

    private void deletePhysicalFile(String materialUrl) {
        if (materialUrl != null && !materialUrl.isEmpty()) {
            try {
                // Extract filename dynamically to ensure perfect path resolution
                String fileName = materialUrl.substring(materialUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get("./uploads/").resolve(fileName);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                System.err.println("Failed to delete physical file: " + e.getMessage());
            }
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteLesson(@PathVariable String id) {
        String courseId = lessonService.getById(id).map(lesson -> {
            deletePhysicalFile(lesson.getMaterialUrl());
            return lesson.getCourseId();
        }).orElse("");
        lessonService.deleteLesson(id);
        return "redirect:/courses/view/" + courseId;
    }

    @PostMapping("/delete-material/{id}")
    public String deleteMaterial(@PathVariable String id, RedirectAttributes redirectAttrs) {
        lessonService.getById(id).ifPresent(lesson -> {
            deletePhysicalFile(lesson.getMaterialUrl());
            lesson.setMaterialUrl(null);
            lesson.setMaterialName(null);
            lessonService.updateLesson(id, lesson);
        });
        redirectAttrs.addFlashAttribute("success", "Lesson material (PDF) has been removed.");
        return "redirect:/lessons/view/" + id;
    }

    @PostMapping("/delete-quiz/{id}")
    public String deleteQuiz(@PathVariable String id) {
        // Cache courseId first to avoid a second DB lookup after the lesson is mutated
        String courseId = lessonService.getById(id).map(lesson -> {
            lesson.setQuiz(new ArrayList<>());
            lessonService.updateLesson(id, lesson);
            return lesson.getCourseId();
        }).orElse("");
        return "redirect:/courses/view/" + courseId;
    }

    @PostMapping("/{id}/add-quiz-question")
    public String addQuizQuestion(@PathVariable String id, @ModelAttribute Lesson.QuizQuestion question) {
        lessonService.getById(id).ifPresent(lesson -> {
            lesson.getQuiz().add(question);
            lessonService.updateLesson(id, lesson);
        });
        return "redirect:/lessons/view/" + id;
    }

    @PostMapping("/{id}/delete-quiz-question/{questionIndex}")
    public String deleteQuizQuestion(@PathVariable String id, @PathVariable int questionIndex) {
        lessonService.getById(id).ifPresent(lesson -> {
            if (lesson.getQuiz() != null && questionIndex >= 0 && questionIndex < lesson.getQuiz().size()) {
                lesson.getQuiz().remove(questionIndex);
                lessonService.updateLesson(id, lesson);
            }
        });
        return "redirect:/lessons/view/" + id;
    }

    @PostMapping("/complete/{id}")
    public String completeLesson(@PathVariable String id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            userService.markLessonAsComplete(userId, id);
        }
        String courseId = lessonService.getById(id).map(Lesson::getCourseId).orElse("");
        return "redirect:/courses/view/" + courseId;
    }

    @PostMapping("/submit-quiz/{id}")
    public String submitQuiz(@PathVariable String id, @RequestParam int earnedPoints, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            userService.submitQuizAndAddPoints(userId, id, earnedPoints);
        }
        String courseId = lessonService.getById(id).map(Lesson::getCourseId).orElse("");
        return "redirect:/courses/view/" + courseId;
    }

}
