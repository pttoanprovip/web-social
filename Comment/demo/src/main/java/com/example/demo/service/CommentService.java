package com.example.demo.service;

import com.example.demo.dto.req.ReplyCommentRequest;
import com.example.demo.dto.req.UpdateCommentRequest;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.req.CreateCommentRequest;
import com.example.demo.dto.res.CommentResponse;
import com.example.demo.event.PostCreateEvent;
import com.example.demo.event.PostDeletedEvent;
import com.example.demo.event.UserUpdatedNameEvent;

public interface CommentService {
    void handlePostCreate(PostCreateEvent event);

    void handlePostDelete(PostDeletedEvent event);

    void handleUserUpdateName(UserUpdatedNameEvent event);

    CommentResponse createComment(CreateCommentRequest req, MultipartFile image, MultipartFile video);

    CommentResponse updateComment(UpdateCommentRequest req, String id, MultipartFile image, MultipartFile video);

    void deleteComment(String id);

    CommentResponse getCommentByPost(String postId);

    CommentResponse replyComment(ReplyCommentRequest req, String parentCommentId, MultipartFile image, MultipartFile video);
}
