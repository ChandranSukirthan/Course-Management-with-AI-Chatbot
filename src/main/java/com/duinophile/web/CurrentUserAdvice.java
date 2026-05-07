package com.duinophile.web;

import com.duinophile.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    private final UserService userService;

    public CurrentUserAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (!(userId instanceof String id) || id.isBlank()) {
            return null;
        }
        boolean viewAsStudent = Boolean.TRUE.equals(session.getAttribute("viewAsStudent"));
        return userService.getUserById(id)
                .map(u -> {
                    com.duinophile.model.User updatedUser = userService.updateAndGetStreak(u);
                    String role = viewAsStudent ? "USER" : updatedUser.getRole();
                    return new CurrentUser(updatedUser.getId(), updatedUser.getUsername(), role, updatedUser.getPoints(), updatedUser.getAvatarUrl(), updatedUser.getStreakCount());
                })
                .orElse(null);
    }

    @ModelAttribute("viewAsStudent")
    public boolean viewAsStudent(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("viewAsStudent"));
    }
}

