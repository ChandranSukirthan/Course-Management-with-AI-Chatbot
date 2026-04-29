package com.duinophile.controller;

import com.duinophile.model.User;
import com.duinophile.service.PostService;
import com.duinophile.service.CommentService;
import com.duinophile.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("view", "register");
        return "layout";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        if (user.getPassword() != null && user.getConfirmPassword() != null 
            && !user.getPassword().equals(user.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
        }
        if (result.hasErrors()) {
            model.addAttribute("view", "register");
            return "layout";
        }
        try {
            userService.registerUser(user);
            return "redirect:/users/login?message=Registration%20successful.%20Please%20log%20in.";
        } catch (RuntimeException ex) {
            model.addAttribute("registerError", ex.getMessage());
            model.addAttribute("view", "register");
            return "layout";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("view", "login");
        return "layout";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        return userService.authenticate(username, password)
                .map(user -> {
                    session.setAttribute("userId", user.getId());
                    if ("ADMIN".equals(user.getRole())) {
                        return "redirect:/admin/dashboard";
                    }
                    if ("STAFF".equals(user.getRole())) {
                        return "redirect:/courses/list";
                    }
                    return "redirect:/users/profile/" + user.getId();
                })
                .orElseGet(() -> {
                    model.addAttribute("loginError", "Invalid username or password");
                    model.addAttribute("view", "login");
                    return "layout";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable String id, Model model) {
        userService.getUserById(id).ifPresent(user -> model.addAttribute("user", user));
        java.util.List<com.duinophile.model.Post> posts = postService.getPostsByAuthor(id);
        for (com.duinophile.model.Post post : posts) {
            post.setComments(commentService.getCommentsByPostId(post.getId()));
        }
        model.addAttribute("userPosts", posts);
        model.addAttribute("view", "profile");
        return "layout";
    }

    @GetMapping("/list")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("view", "users-list");
        return "layout";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        userService.getUserById(id).ifPresent(user -> model.addAttribute("user", user));
        model.addAttribute("view", "edit-user");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable String id, @Valid @ModelAttribute("user") User user, BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("view", "edit-user");
            return "layout";
        }
        userService.updateUser(id, user);
        return "redirect:/users/profile/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return "redirect:/users/list";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("view", "forgot-password");
        return "layout";
    }

    @PostMapping("/verify-identity")
    public String verifyIdentity(@RequestParam String username, @RequestParam String email, HttpSession session, Model model) {
        return userService.findByUsernameAndEmail(username, email)
                .map(user -> {
                    session.setAttribute("resetUserId", user.getId());
                    return "redirect:/users/reset-password";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "No account found matching that username and email.");
                    model.addAttribute("view", "forgot-password");
                    return "layout";
                });
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model) {
        if (session.getAttribute("resetUserId") == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("view", "reset-password");
        return "layout";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String newPassword, @RequestParam String confirmPassword, HttpSession session, Model model) {
        String resetUserId = (String) session.getAttribute("resetUserId");
        if (resetUserId == null) {
            return "redirect:/users/login";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("view", "reset-password");
            return "layout";
        }
        
        if (newPassword.length() < 8 || !newPassword.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$")) {
            model.addAttribute("error", "Password must be at least 8 characters and contain a digit, lowercase, uppercase, and special character.");
            model.addAttribute("view", "reset-password");
            return "layout";
        }

        userService.updatePassword(resetUserId, newPassword);
        session.removeAttribute("resetUserId");
        
        return "redirect:/users/login?message=Password%20successfully%20reset.%20Please%20log%20in.";
    }
}
