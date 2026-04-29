package com.duinophile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username cannot contain spaces or special characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$", message = "Password must contain a digit, lowercase, uppercase, and special character")
    private String password;

    @org.springframework.data.annotation.Transient
    private String confirmPassword;

    private String fullName;
    private String role = "USER"; // Default role
    private long points = 0; // For gamification
    private java.util.List<String> enrolledCourseIds = new java.util.ArrayList<>();
    private java.util.List<String> completedLessonIds = new java.util.ArrayList<>();
    private java.util.List<String> completedQuizLessonIds = new java.util.ArrayList<>();
}
