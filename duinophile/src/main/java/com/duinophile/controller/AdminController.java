package com.duinophile.controller;

import com.duinophile.model.User;
import com.duinophile.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    // ── Guard: only ADMIN role can access ─────────────────────────────
    private boolean isAdmin(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return false;
        return userService.getUserById(userId)
                .map(u -> "ADMIN".equals(u.getRole()))
                .orElse(false);
    }

    // ── Dashboard overview ─────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/users/login";

        model.addAttribute("admins",   userService.getUsersByRole("ADMIN"));
        model.addAttribute("staff",    userService.getUsersByRole("STAFF"));
        model.addAttribute("students", userService.getUsersByRole("USER"));
        model.addAttribute("view", "admin-dashboard");
        return "layout";
    }

    // ── Show add-user form ─────────────────────────────────────────────
    @GetMapping("/add-user")
    public String showAddUserForm(@RequestParam(defaultValue = "USER") String role,
                                  HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/users/login";

        model.addAttribute("newUser", new User());
        model.addAttribute("targetRole", role);
        model.addAttribute("view", "admin-add-user");
        return "layout";
    }

    @PostMapping("/add-user")
    public String addUser(@RequestParam String targetRole,
                          @Valid @ModelAttribute("newUser") User user,
                          BindingResult result,
                          HttpSession session,
                          Model model,
                          RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/users/login";

        // Ensure passwords match
        if (user.getPassword() != null && user.getConfirmPassword() != null 
            && !user.getPassword().equals(user.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.newUser", "Passwords do not match");
        }

        if (result.hasErrors()) {
            model.addAttribute("targetRole", targetRole);
            model.addAttribute("view", "admin-add-user");
            return "layout";
        }

        try {
            userService.createUserWithRole(user, targetRole);
            ra.addFlashAttribute("success", targetRole + " account created successfully!");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/add-user?role=" + targetRole;
        }
        return "redirect:/admin/dashboard";
    }

    // ── Show edit-user form (admin version with role field) ────────────
    @GetMapping("/edit-user/{id}")
    public String showEditUserForm(@PathVariable String id,
                                   HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/users/login";

        userService.getUserById(id).ifPresent(u -> model.addAttribute("editUser", u));
        model.addAttribute("view", "admin-edit-user");
        return "layout";
    }

    @PostMapping("/edit-user/{id}")
    public String editUser(@PathVariable String id,
                           @Valid @ModelAttribute("editUser") User userDetails,
                           BindingResult result,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/users/login";

        // Check if passwords match manually if a new password is being set
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            if (userDetails.getConfirmPassword() == null || !userDetails.getPassword().equals(userDetails.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.editUser", "Passwords do not match");
            }
        }

        if (result.hasErrors()) {
            boolean hasRealErrors = false;
            for (org.springframework.validation.FieldError error : result.getFieldErrors()) {
                if ("password".equals(error.getField()) || "confirmPassword".equals(error.getField())) {
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        hasRealErrors = true;
                        break;
                    }
                } else {
                    hasRealErrors = true;
                    break;
                }
            }
            if (hasRealErrors) {
                model.addAttribute("view", "admin-edit-user");
                return "layout";
            }
        }

        userService.updateUser(id, userDetails);
        ra.addFlashAttribute("success", "User updated successfully!");
        return "redirect:/admin/dashboard";
    }

    // ── Delete user ────────────────────────────────────────────────────
    @PostMapping("/delete-user/{id}")
    public String deleteUser(@PathVariable String id,
                             HttpSession session,
                             RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/users/login";

        userService.deleteUser(id);
        ra.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/dashboard";
    }
}
