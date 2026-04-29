package com.duinophile;

import com.duinophile.model.User;
import com.duinophile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {

        // ── Admin account ────────────────────────────────────────
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@duinophile.com");
            admin.setPassword("admin123");
            admin.setFullName("Admin User");
            admin.setRole("ADMIN");
            admin.setPoints(0L);
            userRepository.save(admin);
            System.out.println("✅ Admin account created     → username: admin   / password: admin123");
        } else {
            System.out.println("ℹ️  Admin account already exists, skipping.");
        }

        // ── Staff account ────────────────────────────────────────
        if (userRepository.findByUsername("staff").isEmpty()) {
            User staff = new User();
            staff.setUsername("staff");
            staff.setEmail("staff@duinophile.com");
            staff.setPassword("staff123");
            staff.setFullName("Staff Member");
            staff.setRole("STAFF");
            staff.setPoints(0L);
            userRepository.save(staff);
            System.out.println("✅ Staff account created     → username: staff   / password: staff123");
        } else {
            System.out.println("ℹ️  Staff account already exists, skipping.");
        }

        // ── Student account ──────────────────────────────────────
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setEmail("student@duinophile.com");
            student.setPassword("student123");
            student.setFullName("Test Student");
            student.setRole("USER");
            student.setPoints(0L);
            userRepository.save(student);
            System.out.println("✅ Student account created   → username: student / password: student123");
        } else {
            System.out.println("ℹ️  Student account already exists, skipping.");
        }
    }
}
