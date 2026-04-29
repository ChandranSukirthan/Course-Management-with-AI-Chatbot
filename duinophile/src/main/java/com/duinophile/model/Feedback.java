package com.duinophile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feedback")
public class Feedback {

    @Id
    private String id;

    @NotBlank(message = "Feedback content is required")
    @jakarta.validation.constraints.Size(min = 10, max = 1000, message = "Feedback must be between 10 and 1000 characters")
    private String content;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private int rating;

    private String courseId; // Reference to Course id
    private String userId; // Reference to User id
    private String username;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String status = "PENDING";
}
