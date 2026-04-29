package com.duinophile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String index(org.springframework.ui.Model model) {
        model.addAttribute("view", "index");
        return "layout";
    }
}
