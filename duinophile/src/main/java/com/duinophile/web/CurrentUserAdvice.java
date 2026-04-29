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
        return userService.getUserById(id)
                .map(u -> new CurrentUser(u.getId(), u.getUsername(), u.getRole(), u.getPoints()))
                .orElse(null);
    }
}

