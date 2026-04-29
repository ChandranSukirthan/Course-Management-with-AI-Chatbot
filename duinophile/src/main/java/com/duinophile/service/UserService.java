package com.duinophile.service;

import com.duinophile.model.User;
import com.duinophile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPassword() != null && u.getPassword().equals(password));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    /** Create a user with an explicit role — used by admin to add STAFF or USER accounts */
    public User createUserWithRole(User user, String role) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    public User updateUser(String id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(userDetails.getFullName());
        user.setEmail(userDetails.getEmail());
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }
        // Allow role to be updated by admin
        if (userDetails.getRole() != null && !userDetails.getRole().isEmpty()) {
            user.setRole(userDetails.getRole());
        }
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    public void addPoints(String userId, long points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }

    public void enrollInCourse(String userId, String courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getEnrolledCourseIds().contains(courseId)) {
            user.getEnrolledCourseIds().add(courseId);
            userRepository.save(user);
        }
    }

    public void markLessonAsComplete(String userId, String lessonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getCompletedLessonIds().contains(lessonId)) {
            user.getCompletedLessonIds().add(lessonId);
            userRepository.save(user);
        }
    }

    public void submitQuizAndAddPoints(String userId, String lessonId, long points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getCompletedQuizLessonIds().contains(lessonId)) {
            user.setPoints(user.getPoints() + points);
            user.getCompletedQuizLessonIds().add(lessonId);
            
            if (!user.getCompletedLessonIds().contains(lessonId)) {
                user.getCompletedLessonIds().add(lessonId);
            }
            userRepository.save(user);
        }
    }

    public Optional<User> findByUsernameAndEmail(String username, String email) {
        return userRepository.findByUsernameAndEmail(username, email);
    }
    
    public void updatePassword(String userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(rawPassword);
        userRepository.save(user);
    }
}
