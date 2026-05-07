package com.duinophile.repository;

import com.duinophile.model.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByAuthorId(String authorId, Sort sort);
    List<Post> findByStatus(String status, Sort sort);
    List<Post> findByStatusOrAuthorId(String status, String authorId, Sort sort);
}
