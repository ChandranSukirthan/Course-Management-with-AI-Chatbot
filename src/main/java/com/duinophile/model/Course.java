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
@Document(collection = "courses")
public class Course {

    @Id
    private String id;

    @NotBlank(message = "Course title is required")
    @jakarta.validation.constraints.Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Course description is required")
    @jakarta.validation.constraints.Size(min = 20, max = 1000, message = "Description must be between 20 and 1000 characters")
    private String description;

    private String creatorId; // Reference to User id
    private boolean isCompleted = false;
    private String level = "BEGINNER"; // BEGINNER, INTERMEDIATE, ADVANCED
    @jakarta.validation.constraints.Min(value = 0, message = "Points cannot be negative")
    private int minimumPointsRequired = 0;
    private List<String> lessonIds = new ArrayList<>(); // References to Lesson ids
}
