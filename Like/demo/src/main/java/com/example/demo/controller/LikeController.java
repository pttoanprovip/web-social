package com.example.demo.controller;

import com.example.demo.dto.req.CreateLikeRequest;
import com.example.demo.dto.req.UpdateLikeRequest;
import com.example.demo.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<?> createLike(@RequestBody CreateLikeRequest req) {
        try{
            return ResponseEntity.ok(likeService.createLike(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLike(@PathVariable String id) {
        try{
            likeService.deleteLike(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLike(@RequestBody UpdateLikeRequest req, @PathVariable String id) {
        try{
            return ResponseEntity.ok(likeService.updateLike(req,id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/group-reactions-by-post/{postId}")
    public ResponseEntity<?> getGroupReactionsByPost(@PathVariable String postId) {
        try{
            return ResponseEntity.ok(likeService.getGroupReactionsByPost(postId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/group-reactions-by-comment/{commmentId}")
    public ResponseEntity<?> getGroupReactionsByComment(@PathVariable String commmentId) {
        try{
            return ResponseEntity.ok(likeService.getGroupReactionsByComment(commmentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/history-me")
    public ResponseEntity<?> getHistoryReactionOfUser() {
        try{
            return ResponseEntity.ok(likeService.getHistoryReactionOfUser());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
