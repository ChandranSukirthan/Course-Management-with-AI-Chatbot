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
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @NotBlank(message = "Comment content is required")
    @jakarta.validation.constraints.Size(min = 2, max = 800, message = "Comment must be between 2 and 800 characters")
    private String content;

    private String postId; // Reference to Post id
    private String authorId; // Reference to User id
    private String authorName;
    @org.springframework.data.mongodb.core.index.Indexed(expireAfter = "30d")
    private LocalDateTime createdAt = LocalDateTime.now();
}
