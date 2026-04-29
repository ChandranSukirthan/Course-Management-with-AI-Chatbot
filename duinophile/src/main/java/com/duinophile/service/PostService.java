package com.duinophile.service;

import com.duinophile.model.Post;
import com.duinophile.repository.PostRepository;
import com.duinophile.web.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public List<Post> getPostsForUser(CurrentUser user) {
        List<Post> allPosts = getAllPosts();
        if (user != null && ("ADMIN".equals(user.role()) || "STAFF".equals(user.role()))) {
            return allPosts;
        }
        // Strict approval filtering for general feed
        return allPosts.stream()
                .filter(p -> "APPROVED".equals(p.getStatus()) && p.isPubliclyVisible())
                .collect(Collectors.toList());
    }

    public void approvePost(String id) {
        postRepository.findById(id).ifPresent(post -> {
            post.setStatus("APPROVED");
            postRepository.save(post);
        });
    }

    public Optional<Post> getPostById(String id) {
        return postRepository.findById(id);
    }

    public Post updatePost(String id, Post postDetails) {
        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());
        post.setLevel(postDetails.getLevel());
        post.setPubliclyVisible(postDetails.isPubliclyVisible());
        if (postDetails.getAchievementType() != null) {
            post.setAchievementType(postDetails.getAchievementType());
        }
        if (postDetails.getImageUrl() != null) {
            post.setImageUrl(postDetails.getImageUrl());
        }

        // If it's private, it's auto-approved for their own profile.
        // If it's public, it needs admin moderation (PENDING).
        if (!post.isPubliclyVisible()) {
            post.setStatus("APPROVED");
        } else {
            post.setStatus("PENDING");
        }

        return postRepository.save(post);
    }

    public List<Post> getPostsByAuthor(String authorId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    public void deletePost(String id) {
        postRepository.deleteById(id);
    }

    public void toggleReaction(String postId, String userId, String reactionType) {
        postRepository.findById(postId).ifPresent(post -> {
            java.util.Map<String, java.util.Set<String>> reactions = post.getReactions();
            if (reactions == null) {
                reactions = new java.util.HashMap<>();
                post.setReactions(reactions);
            }

            boolean alreadyHasThis = false;
            for (java.util.Map.Entry<String, java.util.Set<String>> entry : reactions.entrySet()) {
                if (entry.getValue() != null && entry.getValue().contains(userId)) {
                    if (entry.getKey().equals(reactionType)) {
                        alreadyHasThis = true;
                    }
                    entry.getValue().remove(userId);
                }
            }

            if (!alreadyHasThis) {
                reactions.computeIfAbsent(reactionType, k -> new java.util.HashSet<>()).add(userId);
            }

            postRepository.save(post);
        });
    }
}
