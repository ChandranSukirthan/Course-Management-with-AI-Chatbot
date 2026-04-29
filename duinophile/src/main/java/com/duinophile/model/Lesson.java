package com.duinophile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lessons")
public class Lesson {

    @Id
    private String id;

    @NotBlank(message = "Lesson title is required")
    @jakarta.validation.constraints.Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Lesson content is required")
    @jakarta.validation.constraints.Size(min = 20, max = 15000, message = "Content must be between 20 and 15000 characters")
    private String content;

    private String courseId;
    private List<QuizQuestion> quiz = new ArrayList<>();
    private String materialUrl; // Path to PDF
    private String materialName; // Original file name

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestion {
        @NotBlank(message = "Question cannot be blank")
        @jakarta.validation.constraints.Size(min = 10, max = 500, message = "Quiz question must be between 10 and 500 characters")
        private String question;
        
        @jakarta.validation.constraints.Size(max = 2000, message = "Content cannot exceed 2000 characters")
        private String content; // Context or explanation material for this specific question

        private List<String> options = new ArrayList<>();
        private int correctOptionIndex;
        @jakarta.validation.constraints.Min(value = 1, message = "Points must be at least 1")
        @jakarta.validation.constraints.Max(value = 100, message = "Points cannot exceed 100")
        private int points = 10; // Points awarded for a correct answer
    }
}
