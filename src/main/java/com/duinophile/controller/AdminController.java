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

        // Validate password match only when a new password is being set
        String newPassword = userDetails.getPassword();
        boolean changingPassword = newPassword != null && !newPassword.isBlank();
        if (changingPassword) {
            if (userDetails.getConfirmPassword() == null
                    || !newPassword.equals(userDetails.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.editUser", "Passwords do not match");
            }
        }

        if (result.hasErrors()) {
            // Ignore password-field validation errors when the admin is not changing the password
            boolean hasRealError = result.getFieldErrors().stream().anyMatch(e -> {
                String f = e.getField();
                if (("password".equals(f) || "confirmPassword".equals(f)) && !changingPassword) {
                    return false; // not a real error — password field is optional on edit
                }
                return true;
            });
            if (hasRealError) {
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

        userService.deleteUserAndData(id);
        ra.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/dashboard";
    }

    // ── View as Student masquerade ─────────────────────────────────────
    @GetMapping("/view-as-student")
    public String viewAsStudent(HttpSession session) {
        if (!isAdmin(session)) return "redirect:/users/login";
        session.setAttribute("viewAsStudent", true);
        return "redirect:/";
    }

    @GetMapping("/restore-admin")
    public String restoreAdmin(HttpSession session) {
        session.removeAttribute("viewAsStudent");
        return "redirect:/admin/dashboard";
    }
}
