package com.example.demo.repo;

import com.example.demo.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    void deletePostById(String postId);

    List<Comment> findByPostId(String postId);
}
