package com.example.demo.repo;

import com.example.demo.enums.Type;
import com.example.demo.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, String> {
    void deleteByPostId(String postId);

    Optional<Like> findByUserIdAndPostId(String userId, String postId);

    Optional<Like> findByUserIdAndCommentId(String userId, String commentId);

    List<Like> findByPostId(String postId);

    @Query("select l from Like l where l.userId = ?1 order by l.createAt")
    List<Like> findByUserId(String userId);

    List<Like> findByCommentId(String commentId);
}
