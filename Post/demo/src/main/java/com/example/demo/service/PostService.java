package com.example.demo.service;

import com.example.demo.dto.req.PostCreateRequest;
import com.example.demo.dto.req.UpdatePostRequest;
import com.example.demo.event.UserRegisteredEvent;
import com.example.demo.dto.res.ListPostResponse;
import com.example.demo.dto.res.PostResponse;
import com.example.demo.event.UserUpdatedNameEvent;

import java.util.List;

public interface PostService {

    void handleUserRegistered(UserRegisteredEvent event);

    void handleUserUpdatedName(UserUpdatedNameEvent event);

    PostResponse createPost(PostCreateRequest req);

    PostResponse getPostById(String id);

    PostResponse updatePost(UpdatePostRequest req, String id);

    void deletePost(String id);

    List<PostResponse> getAllPosts();

    ListPostResponse getAllPostsOfUser();
}
