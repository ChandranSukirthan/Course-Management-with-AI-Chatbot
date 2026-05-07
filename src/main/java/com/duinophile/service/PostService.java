package com.duinophile.service;

import com.duinophile.model.Post;
import com.duinophile.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    // ── Write ─────────────────────────────────────────────────────────────

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    /**
     * Updates all user-editable fields on a post.
     * Previously only content and imageUrl were propagated;
     * title, achievementType, and publiclyVisible were silently dropped.
     */
    public Post updatePost(String id, Post postDetails) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());
        post.setAchievementType(postDetails.getAchievementType());
        post.setPubliclyVisible(postDetails.isPubliclyVisible());
        if (postDetails.getImageUrl() != null && !postDetails.getImageUrl().isBlank()) {
            post.setImageUrl(postDetails.getImageUrl());
        }
        return postRepository.save(post);
    }

    public void deletePost(String id) {
        postRepository.deleteById(id);
    }

    public void approvePost(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setStatus("APPROVED");
        postRepository.save(post);
    }

    public void toggleReaction(String postId, String userId, String reactionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.getReactions() == null) {
            post.setReactions(new java.util.HashMap<>());
        }

        List<String> reactors = post.getReactions()
                .computeIfAbsent(reactionType, k -> new java.util.ArrayList<>());

        if (reactors.contains(userId)) {
            reactors.remove(userId);
        } else {
            reactors.add(userId);
        }
        postRepository.save(post);
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<Post> getAllPosts() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Feed shows ONLY APPROVED posts. 
     * Authors must go to their profile to see their pending posts.
     */
    public List<Post> getFeedForUser(String userId) {
        return getAllPosts().stream()
                .filter(p -> "APPROVED".equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Returns posts by author. If viewer is not the author or admin, only returns APPROVED posts.
     */
    public List<Post> getPostsByAuthor(String authorId, String viewerId, String viewerRole) {
        List<Post> posts = postRepository.findByAuthorId(authorId, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (authorId.equals(viewerId) || "ADMIN".equals(viewerRole) || "STAFF".equals(viewerRole)) {
            return posts;
        }
        return posts.stream()
                .filter(p -> "APPROVED".equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Post> getPendingPosts() {
        return postRepository.findByStatus("PENDING", Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Optional<Post> getPostById(String id) {
        return postRepository.findById(id);
    }
}
