package com.duinophile.repository;

import com.duinophile.model.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId);
}
