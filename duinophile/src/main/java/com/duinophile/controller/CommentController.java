package com.duinophile.controller;

import com.duinophile.model.Comment;
import com.duinophile.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.duinophile.web.CurrentUser;

@Controller
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/create")
    public String createComment(@jakarta.validation.Valid @ModelAttribute Comment comment, org.springframework.validation.BindingResult result, @ModelAttribute("currentUser") CurrentUser currentUser, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs, @RequestHeader(value = "Referer", required = false) String referer) {
        String target = referer != null ? "redirect:" + referer : "redirect:/posts/feed";

        if (result.hasErrors()) {
            redirectAttrs.addFlashAttribute("error", "Comment must be between 2 and 800 characters.");
            return target;
        }

        if (currentUser != null) {
            java.util.List<Comment> postComments = commentService.getCommentsByPostId(comment.getPostId());
            
            long personalCommentCount = postComments.stream()
                .filter(c -> currentUser.id().equals(c.getAuthorId()))
                .count();
                
            if (personalCommentCount >= 3) {
                redirectAttrs.addFlashAttribute("error", "Comment Limit: You can only post a maximum of 3 comments per discussion thread.");
                return target;
            }

            java.util.Optional<Comment> lastComment = postComments.stream()
                .filter(c -> currentUser.id().equals(c.getAuthorId()))
                .reduce((first, second) -> second); // Gets the absolute latest comment in the list

            if (lastComment.isPresent() && lastComment.get().getContent().equals(comment.getContent())) {
                redirectAttrs.addFlashAttribute("error", "Spam protection: You just posted this exact comment.");
                return target;
            }

            comment.setAuthorId(currentUser.id());
            comment.setAuthorName(currentUser.username());
        } else {
            redirectAttrs.addFlashAttribute("error", "You must be logged in to comment.");
            return "redirect:/login";
        }
        
        commentService.createComment(comment);
        redirectAttrs.addFlashAttribute("success", "Comment posted successfully!");
        return target;
    }

    @GetMapping("/edit/{id}")
    public String showEditCommentForm(@PathVariable String id, Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Comment comment = commentService.getCommentById(id).orElse(null);
        if (comment == null || currentUser == null || !currentUser.id().equals(comment.getAuthorId())) {
            return "redirect:/posts/feed";
        }
        model.addAttribute("comment", comment);
        model.addAttribute("view", "edit-comment");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updateComment(@PathVariable String id, @ModelAttribute Comment comment, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Comment existing = commentService.getCommentById(id).orElse(null);
        if (existing != null && currentUser != null && currentUser.id().equals(existing.getAuthorId())) {
            commentService.updateComment(id, comment);
        }
        return "redirect:/posts/feed";
    }

    @PostMapping("/delete/{id}")
    public String deleteComment(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Comment comment = commentService.getCommentById(id).orElse(null);
        if (comment == null) return "redirect:/posts/feed";
        
        boolean canDelete = currentUser != null && (
            "ADMIN".equals(currentUser.role()) || 
            "STAFF".equals(currentUser.role()) || 
            currentUser.id().equals(comment.getAuthorId())
        );
        
        if (canDelete) {
            commentService.deleteComment(id);
        }
        return "redirect:/posts/view/" + comment.getPostId();
    }
}
