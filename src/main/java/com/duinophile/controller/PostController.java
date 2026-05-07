package com.duinophile.controller;

import com.duinophile.model.Post;
import com.duinophile.service.PostService;
import com.duinophile.service.CommentService;
import com.duinophile.web.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @GetMapping("/feed")
    public String showFeed(Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        String userId = (currentUser != null) ? currentUser.id() : null;
        List<Post> feed = postService.getFeedForUser(userId);
        
        // Populate comments for each post
        for (Post post : feed) {
            post.setComments(commentService.getCommentsByPostId(post.getId()));
        }
        
        model.addAttribute("posts", feed);
        model.addAttribute("view", "feed");
        return "layout";
    }

    @GetMapping("/create")
    public String showCreatePostForm(Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        if (currentUser == null) {
            return "redirect:/users/login";
        }
        model.addAttribute("post", new Post());
        model.addAttribute("view", "create-post");
        return "layout";
    }

    @PostMapping("/create")
    public String createPost(@Valid @ModelAttribute Post post, BindingResult result,
                             @ModelAttribute("currentUser") CurrentUser currentUser,
                             @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                             Model model, RedirectAttributes redirectAttrs) {
        if (currentUser == null) {
            return "redirect:/users/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("view", "create-post");
            return "layout";
        }

        post.setAuthorId(currentUser.id());
        post.setAuthorName(currentUser.username());

        // Handle image upload
        handleImageUpload(post, imageFile);

        if ("ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role())) {
            post.setStatus("APPROVED");
        } else {
            post.setStatus("PENDING");
        }

        postService.createPost(post);
        redirectAttrs.addFlashAttribute("success", "Post created successfully! " + ("PENDING".equals(post.getStatus()) ? "It is awaiting approval." : ""));
        return "redirect:/users/profile/" + currentUser.id();
    }

    @GetMapping("/view/{id}")
    public String viewPost(@PathVariable String id, Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Post post = postService.getPostById(id).orElse(null);
        if (post == null) {
            return "redirect:/posts/feed";
        }
        
        // Restriction: If pending, only author/admin/staff can view
        if ("PENDING".equals(post.getStatus())) {
            if (currentUser == null || (!currentUser.id().equals(post.getAuthorId()) && !"ADMIN".equals(currentUser.role()) && !"STAFF".equals(currentUser.role()))) {
                return "redirect:/posts/feed";
            }
        }

        post.setComments(commentService.getCommentsByPostId(post.getId()));
        model.addAttribute("post", post);
        model.addAttribute("view", "post-details");
        return "layout";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        Post post = postService.getPostById(id).orElse(null);
        if (post == null || currentUser == null || !currentUser.id().equals(post.getAuthorId())) {
            return "redirect:/posts/feed";
        }
        model.addAttribute("post", post);
        model.addAttribute("view", "edit-post");
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updatePost(@PathVariable String id, @Valid @ModelAttribute Post postDetails,
                             BindingResult result, @ModelAttribute("currentUser") CurrentUser currentUser,
                             @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
                             Model model, RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("view", "edit-post");
            return "layout";
        }
        Post existing = postService.getPostById(id).orElse(null);
        if (existing != null && currentUser != null && currentUser.id().equals(existing.getAuthorId())) {
            // Keep existing image if no new file is uploaded
            if (imageFile == null || imageFile.isEmpty()) {
                postDetails.setImageUrl(existing.getImageUrl());
            } else {
                handleImageUpload(postDetails, imageFile);
            }
            postService.updatePost(id, postDetails);
            redirectAttrs.addFlashAttribute("success", "Post updated successfully!");
        }
        return "redirect:/posts/view/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser, 
                             @RequestHeader(value = "Referer", required = false) String referer,
                             RedirectAttributes redirectAttrs) {
        Post post = postService.getPostById(id).orElse(null);
        if (post != null && currentUser != null && 
            ("ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role()) || currentUser.id().equals(post.getAuthorId()))) {
            postService.deletePost(id);
            redirectAttrs.addFlashAttribute("success", "Post deleted successfully.");
        }
        return referer != null ? "redirect:" + referer : "redirect:/posts/feed";
    }

    @PostMapping("/react/{id}")
    public String reactToPost(@PathVariable String id, @RequestParam String type, 
                              @ModelAttribute("currentUser") CurrentUser currentUser, 
                              @RequestHeader(value = "Referer", required = false) String referer) {
        if (currentUser != null) {
            postService.toggleReaction(id, currentUser.id(), type);
        }
        return referer != null ? "redirect:" + referer : "redirect:/posts/feed";
    }

    @GetMapping("/manage")
    public String manageFeed(Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        if (currentUser == null || (!"ADMIN".equals(currentUser.role()) && !"STAFF".equals(currentUser.role()))) {
            return "redirect:/posts/feed";
        }
        model.addAttribute("pendingPosts", postService.getPendingPosts());
        model.addAttribute("view", "manage-feed");
        return "layout";
    }

    @PostMapping("/approve/{id}")
    public String approvePost(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser,
                              @RequestHeader(value = "Referer", required = false) String referer,
                              RedirectAttributes redirectAttrs) {
        if (currentUser != null && ("ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role()))) {
            postService.approvePost(id);
            redirectAttrs.addFlashAttribute("success", "Post approved.");
        }
        return referer != null ? "redirect:" + referer : "redirect:/posts/feed";
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    private void handleImageUpload(Post post, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("image/"))) {
            return; // silently skip non-image files
        }

        try {
            Path uploadDir = Paths.get("./uploads");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName));
            post.setImageUrl("/uploads/" + fileName);
        } catch (IOException e) {
            System.err.println("[PostController] Image upload failed: " + e.getMessage());
        }
    }
}
