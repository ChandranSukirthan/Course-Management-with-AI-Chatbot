package com.duinophile.service;

import com.duinophile.model.User;
import com.duinophile.model.Post;
import com.duinophile.model.Comment;
import com.duinophile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    // Shared BCrypt encoder — strength 10 is production standard
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    // ── Registration ─────────────────────────────────────────────────────

    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken. Please choose another.");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("An account with that email already exists.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    /** Create a user with an explicit role — used by admin to add STAFF or USER accounts */
    public User createUserWithRole(User user, String role) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken.");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("An account with that email already exists.");
        }
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // ── Authentication (with transparent BCrypt migration) ───────────────

    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        String stored = user.getPassword();
        if (stored == null) return Optional.empty();

        boolean matches;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            // Already BCrypt — use encoder
            matches = passwordEncoder.matches(password, stored);
        } else {
            // Legacy plaintext — compare directly then transparently migrate
            matches = stored.equals(password);
            if (matches) {
                // Silently upgrade to BCrypt on successful login
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }
        return matches ? Optional.of(user) : Optional.empty();
    }

    // ── Lookup ───────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsernameAndEmail(String username, String email) {
        return userRepository.findByUsernameAndEmail(username, email);
    }

    // ── Update ───────────────────────────────────────────────────────────

    public User updateUser(String id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(userDetails.getFullName());
        user.setEmail(userDetails.getEmail());
        // Only update password if a new one is explicitly provided
        if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        // Allow admin to update role
        if (userDetails.getRole() != null && !userDetails.getRole().isBlank()) {
            user.setRole(userDetails.getRole());
        }
        return userRepository.save(user);
    }

    public void updatePassword(String userId, String rawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    // ── Points & Progress ────────────────────────────────────────────────

    public void addPoints(String userId, long points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }

    public void enrollInCourse(String userId, String courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getEnrolledCourseIds() == null) {
            user.setEnrolledCourseIds(new java.util.ArrayList<>());
        }
        if (!user.getEnrolledCourseIds().contains(courseId)) {
            user.getEnrolledCourseIds().add(courseId);
            userRepository.save(user);
        }
    }

    public void markLessonAsComplete(String userId, String lessonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getCompletedLessonIds() == null) {
            user.setCompletedLessonIds(new java.util.ArrayList<>());
        }
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

    // ── Streaks ──────────────────────────────────────────────────────────

    public User updateAndGetStreak(User user) {
        java.time.LocalDate today = java.time.LocalDate.now();
        boolean needsSave = false;

        // 1. Reset freezes per month
        if (user.getLastFreezeResetDate() == null || user.getLastFreezeResetDate().getMonth() != today.getMonth() || user.getLastFreezeResetDate().getYear() != today.getYear()) {
            user.setStreakFreezes(4);
            user.setLastFreezeResetDate(today);
            needsSave = true;
        }

        // 2. Streak Logic
        if (user.getLastActiveDate() == null) {
            user.setLastActiveDate(today);
            user.setStreakCount(1);
            needsSave = true;
        } else if (!user.getLastActiveDate().equals(today)) {
            long daysMissed = java.time.temporal.ChronoUnit.DAYS.between(user.getLastActiveDate(), today) - 1;
            
            if (daysMissed == 0) {
                // Logged in yesterday, streak continues!
                user.setStreakCount(user.getStreakCount() + 1);
            } else if (daysMissed > 0) {
                // Missed one or more days
                if (user.getStreakFreezes() >= daysMissed) {
                    // Consume freezes to keep streak alive and increment for today
                    user.setStreakFreezes(user.getStreakFreezes() - (int) daysMissed);
                    user.setStreakCount(user.getStreakCount() + 1);
                } else {
                    // Not enough freezes, streak breaks
                    user.setStreakCount(1);
                    // Freezes remain the same or reset? Usually they just stay at what they are (which is less than days missed).
                }
            }
            // If daysMissed < 0, it means lastActiveDate is in the future. We just reset to today.
            user.setLastActiveDate(today);
            needsSave = true;
        }

        if (needsSave) {
            userRepository.save(user);
        }
        return user;
    }

    // ── Deletion ─────────────────────────────────────────────────────────

    public void deleteUserAndData(String id) {
        // Anonymize this user's posts and comments so community data is preserved
        Query query = new Query(Criteria.where("authorId").is(id));
        Update update = new Update()
                .set("authorName", "[Deactivated Account]")
                .set("authorId", null);
        mongoTemplate.updateMulti(query, update, Post.class);
        mongoTemplate.updateMulti(query, update, Comment.class);
        userRepository.deleteById(id);
    }

    // ── Avatar ──────────────────────────────────────────────────────────────

    public String saveAvatar(String userId, org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Store avatars in a dedicated sub-folder for organisation
        java.nio.file.Path avatarDir = java.nio.file.Paths.get("./uploads/avatars");
        if (!java.nio.file.Files.exists(avatarDir)) {
            java.nio.file.Files.createDirectories(avatarDir);
        }

        // Delete previous avatar to avoid orphan files
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            String oldFile = user.getAvatarUrl().substring(user.getAvatarUrl().lastIndexOf("/") + 1);
            java.nio.file.Path oldPath = avatarDir.resolve(oldFile);
            java.nio.file.Files.deleteIfExists(oldPath);
        }

        // Generate a unique filename preserving the original extension
        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
        String fileName = java.util.UUID.randomUUID().toString() + ext;

        java.nio.file.Files.copy(file.getInputStream(), avatarDir.resolve(fileName));

        String avatarUrl = "/uploads/avatars/" + fileName;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }

    // ── Helper (used by controllers for account-deletion password check) ──

    public boolean verifyPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // Legacy plaintext fallback
        return storedPassword.equals(rawPassword);
    }
}
