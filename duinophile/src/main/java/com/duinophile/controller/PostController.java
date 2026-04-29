package com.duinophile.controller;

import com.duinophile.model.Post;
import com.duinophile.service.PostService;
import com.duinophile.service.CommentService;
import com.duinophile.web.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        java.util.List<Post> allPosts = postService.getAllPosts();
        java.util.List<Post> approvedPublicPosts = allPosts.stream()
                .filter(p -> "APPROVED".equals(p.getStatus()) && p.isPubliclyVisible())
                .collect(java.util.stream.Collectors.toList());

        for (Post post : approvedPublicPosts) {
            post.setComments(commentService.getCommentsByPostId(post.getId()));
        }
        model.addAttribute("posts", approvedPublicPosts);
        model.addAttribute("view", "feed");
        return "layout";
    }

    @GetMapping("/manage")
    public String showManageFeed(Model model, @ModelAttribute("currentUser") CurrentUser currentUser) {
        if (currentUser == null || (!"ADMIN".equals(currentUser.role()) && !"STAFF".equals(currentUser.role()))) {
            return "redirect:/posts/feed";
        }
        java.util.List<Post> allPosts = postService.getAllPosts();
        java.util.List<Post> pendingPublicPosts = allPosts.stream()
                .filter(p -> "PENDING".equals(p.getStatus()) && p.isPubliclyVisible())
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("pendingPosts", pendingPublicPosts);
        model.addAttribute("view", "manage-feed");
        return "layout";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("newPost", new Post());
        model.addAttribute("view", "create-post");
        return "layout";
    }

    @PostMapping("/create")
    public String createPost(@jakarta.validation.Valid @ModelAttribute Post post, org.springframework.validation.BindingResult result,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             @ModelAttribute("currentUser") CurrentUser currentUser, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            String errorMsg = result.getAllErrors().get(0).getDefaultMessage();
            redirectAttrs.addFlashAttribute("error", errorMsg);
            return "redirect:/posts/create";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageFile.getSize() > 20 * 1024 * 1024) {
                redirectAttrs.addFlashAttribute("error", "The uploaded image must be less than 20MB.");
                return "redirect:/posts/create";
            }
            String fileName = imageFile.getOriginalFilename();
            if (fileName != null) {
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                if (!java.util.Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
                    redirectAttrs.addFlashAttribute("error", "Only JPG, PNG and GIF formats are allowed.");
                    return "redirect:/posts/create";
                }
            }
        }

        if (currentUser != null) {
            // Anti-Spam: Block Exact Twin Duplications
            java.util.List<Post> recentPosts = postService.getPostsForUser(currentUser);
            java.util.Optional<Post> lastPost = recentPosts.stream()
                    .filter(p -> currentUser.id().equals(p.getAuthorId()))
                    .findFirst();

            if (lastPost.isPresent() && lastPost.get().getContent().equals(post.getContent())) {
                redirectAttrs.addFlashAttribute("error", "Spam protection: You just posted this exact content.");
                return "redirect:/posts/feed";
            }

            post.setAuthorId(currentUser.id());
            post.setAuthorName(currentUser.username());
        } else {
            redirectAttrs.addFlashAttribute("error", "You must be logged in to post.");
            return "redirect:/login";
        }

        handleImageUpload(post, imageFile);
        if (!post.isPubliclyVisible()) {
            post.setStatus("APPROVED");
        }
        postService.createPost(post);
        redirectAttrs.addFlashAttribute("success", post.isPubliclyVisible() ?
                "Post submitted! It will be visible to everyone after admin approval." :
                "Post saved successfully to your profile!");
        return "redirect:/users/profile/" + currentUser.id();
    }

    @PostMapping("/approve/{id}")
    public String approvePost(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser) {
        if (currentUser != null && ("ADMIN".equals(currentUser.role()) || "STAFF".equals(currentUser.role()))) {
            postService.approvePost(id);
        }
        return "redirect:/posts/manage";
    }

    @GetMapping("/view/{id}")
    public String viewPost(@PathVariable String id, Model model) {
        postService.getPostById(id).ifPresent(post -> {
            model.addAttribute("post", post);
            model.addAttribute("comments", commentService.getCommentsByPostId(id));
            model.addAttribute("newComment", new com.duinophile.model.Comment());
        });
        model.addAttribute("view", "post-details");
        return "layout";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Model model) {
        postService.getPostById(id).ifPresent(post -> model.addAttribute("post", post));
        model.addAttribute("view", "edit-post"); // Ensure this template exists
        return "layout";
    }

    @PostMapping("/update/{id}")
    public String updatePost(@PathVariable String id, @jakarta.validation.Valid @ModelAttribute Post post, org.springframework.validation.BindingResult result,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            String errorMsg = result.getAllErrors().get(0).getDefaultMessage();
            redirectAttrs.addFlashAttribute("error", errorMsg);
            return "redirect:/posts/edit/" + id;
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageFile.getSize() > 20 * 1024 * 1024) {
                redirectAttrs.addFlashAttribute("error", "The uploaded image must be less than 20MB.");
                return "redirect:/posts/edit/" + id;
            }
            String fileName = imageFile.getOriginalFilename();
            if (fileName != null) {
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                if (!java.util.Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
                    redirectAttrs.addFlashAttribute("error", "Only JPG, PNG and GIF formats are allowed.");
                    return "redirect:/posts/edit/" + id;
                }
            }
        }

        handleImageUpload(post, imageFile);
        if (!post.isPubliclyVisible()) {
            post.setStatus("APPROVED");
        } else {
            // If it was previously APPROVED but now PUBLICly visible again, it might need re-approval?
            // Actually, let's just make it PENDING if it's public.
            post.setStatus("PENDING");
        }
        postService.updatePost(id, post);
        return "redirect:/posts/view/" + id;
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable String id, @ModelAttribute("currentUser") CurrentUser currentUser) {
        postService.getPostById(id).ifPresent(post -> {
            boolean canDelete = currentUser != null && (
                    "ADMIN".equals(currentUser.role()) ||
                            "STAFF".equals(currentUser.role()) ||
                            currentUser.id().equals(post.getAuthorId())
            );
            if (canDelete) {
                postService.deletePost(id);
            }
        });
        return "redirect:/posts/feed";
    }

    @PostMapping("/react/{id}")
    public String reactToPost(@PathVariable String id, @RequestParam String type, @ModelAttribute("currentUser") CurrentUser currentUser, @RequestHeader(value = "Referer", required = false) String referer) {
        if (currentUser != null) {
            postService.toggleReaction(id, currentUser.id(), type);
        }
        return referer != null ? "redirect:" + referer : "redirect:/posts/feed";
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(org.springframework.web.multipart.MaxUploadSizeExceededException exc, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("error", "The uploaded image exceeds the maximum size limit.");
        return "redirect:/posts/feed";
    }

    private void handleImageUpload(Post post, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                String uploadsDir = "./uploads/";
                Path uploadPath = Paths.get(uploadsDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath);

                post.setImageUrl("/uploads/" + fileName);
            } catch (IOException e) {
                System.err.println("Failed to upload image: " + e.getMessage());
            }
        }
    }
}
