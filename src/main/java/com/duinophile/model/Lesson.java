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
    @jakarta.validation.constraints.Size(min = 5, max = 50, message = "Title must be between 5 and 50 characters")
    private String title;

    @NotBlank(message = "Lesson content is required")
    @jakarta.validation.constraints.Size(min = 20, max = 5000, message = "Content must be between 20 and 5000 characters")
    private String content;

    private String courseId;

    @jakarta.validation.Valid
    private List<QuizQuestion> quiz = new ArrayList<>();
    private String materialUrl; // Path to PDF
    private String materialName; // Original file name

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizQuestion {
        @NotBlank(message = "Question cannot be blank")
        @jakarta.validation.constraints.Size(min = 10, max = 75, message = "Quiz question must be between 10 and 75 characters")
        private String question;

        @jakarta.validation.constraints.Size(min = 10, max = 200, message = "Content must be between 10 and 200 characters (Optional)")
        private String content; // Context or explanation material for this specific question

        public void setContent(String content) {
            this.content = (content == null || content.trim().isEmpty()) ? null : content.trim();
        }

        private List<String> options = new ArrayList<>();
        private int correctOptionIndex;
        @jakarta.validation.constraints.Min(value = 0, message = "Points cannot be a negative number")
        @jakarta.validation.constraints.Max(value = 100, message = "Points cannot exceed 100")
        private int points = 10; // Points awarded for a correct answer
    }
}
