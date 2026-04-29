package com.duinophile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Post content is required")
    @jakarta.validation.constraints.Size(min = 10, max = 3000, message = "Post content must be between 10 and 3000 characters")
    private String content;

    @NotBlank(message = "Achievement type is required")
    private String achievementType;
    private String imageUrl;

    @jakarta.validation.constraints.Min(value = 0, message = "Level must be at least 0")
    @jakarta.validation.constraints.Max(value = 20, message = "Level cannot exceed 20")
    private Integer level;

    private boolean publiclyVisible = true;

    private String authorId; // Reference to User id
    private String authorName;
    @org.springframework.data.mongodb.core.index.Indexed(expireAfter = "60d")
    private LocalDateTime createdAt = LocalDateTime.now();
    private java.util.Map<String, java.util.Set<String>> reactions = new java.util.HashMap<>();
    private String status = "PENDING";

    @org.springframework.data.annotation.Transient
    private java.util.List<Comment> comments;
}
