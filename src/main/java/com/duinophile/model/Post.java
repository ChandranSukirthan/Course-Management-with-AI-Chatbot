package com.duinophile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    private String authorId;
    private String authorName;

    private String title;

    @NotBlank(message = "Post content cannot be empty")
    private String content;

    private String achievementType;
    private Integer level;
    private boolean publiclyVisible = true;
    private String imageUrl;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    // Key is reaction type (like, love, insight), Value is list of user IDs
    private Map<String, List<String>> reactions = new HashMap<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private List<Comment> comments = new ArrayList<>();
}
