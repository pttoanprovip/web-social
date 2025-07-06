package com.example.demo.controller;

import com.example.demo.dto.req.CreateCommentRequest;
import com.example.demo.dto.req.ReplyCommentRequest;
import com.example.demo.dto.req.UpdateCommentRequest;
import com.example.demo.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/create")
    public ResponseEntity<?> createPost(
            @RequestPart CreateCommentRequest req,
            @RequestPart(required = false) MultipartFile image,
            @RequestPart(required = false) MultipartFile video) {
        try {
            return ResponseEntity.ok(commentService.createComment(req, image, video));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(
            @RequestPart UpdateCommentRequest req,
            @PathVariable String id,
            @RequestPart(required = false) MultipartFile image,
            @RequestPart(required = false) MultipartFile video) {
        try {
            return ResponseEntity.ok(commentService.updateComment(req, id, image, video));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable String id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getCommentByPost(@PathVariable String postId) {
        try {
            return ResponseEntity.ok(commentService.getCommentByPost(postId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{parentCommentId}/reply")
    public ResponseEntity<?> replyComment(
            @PathVariable String parentCommentId,
            @RequestPart ReplyCommentRequest req,
            @RequestPart(required = false) MultipartFile image,
            @RequestPart(required = false) MultipartFile video) {
        try {
            return ResponseEntity.ok(commentService.replyComment(req, parentCommentId, image, video));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
