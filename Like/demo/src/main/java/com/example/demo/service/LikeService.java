package com.example.demo.service;

import com.example.demo.dto.req.CreateLikeRequest;
import com.example.demo.dto.req.UpdateLikeRequest;
import com.example.demo.dto.res.LikeResponse;
import com.example.demo.event.PostCreateEvent;
import com.example.demo.event.PostDeletedEvent;

import java.util.List;
import java.util.Map;

public interface LikeService {

    void handlePostCreate(PostCreateEvent event);

    void handlePostDelete(PostDeletedEvent event);

    LikeResponse createLike(CreateLikeRequest req);

    void deleteLike(String id);

    LikeResponse updateLike(UpdateLikeRequest req, String id);

    Map<String, Object> getGroupReactionsByPost(String postId);

    LikeResponse getHistoryReactionOfUser();
}
