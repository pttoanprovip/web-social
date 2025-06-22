package com.example.demo.service.Impl;

import com.example.demo.config.CloudinaryConfig;
import com.example.demo.dto.req.PostCreateRequest;
import com.example.demo.dto.req.UpdatePostRequest;
import com.example.demo.dto.res.*;
import com.example.demo.event.UserRegisteredEvent;
import com.example.demo.event.UserUpdatedNameEvent;
import com.example.demo.exception.PostException;
import com.example.demo.model.UserCache;
import com.example.demo.repo.PostRepository;
import com.example.demo.repo.UserCacheRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;

import com.example.demo.model.Post;
import com.example.demo.service.PostService;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserCacheRepository userCacheRepository;
    private final RestTemplate restTemplate;
    private final CloudinaryConfig cloudinaryConfig;

    @Override
    @KafkaListener(topics = "user-registered", groupId = "Post", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserRegistered(UserRegisteredEvent event) {
        System.out.println("User registered: " + event.getId() + " - " + event.getFName() + " - " + event.getLName() + " - " + event.getAvatar());
        UserCache userCache = UserCache.builder().id(event.getId()).fName(event.getFName()).lName(event.getLName()).avatar(event.getAvatar()).build();
        userCacheRepository.save(userCache);
    }

    @Override
    @KafkaListener(topics = "user-updated_name", groupId = "Post", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserUpdatedName(UserUpdatedNameEvent event) {
        UserCache user = UserCache.builder().id(event.getId()).fName(event.getFName()).lName(event.getLName()).avatar(event.getAvatar()).build();
        userCacheRepository.save(user);
    }

    @Override
    public PostResponse createPost(PostCreateRequest req, MultipartFile image, MultipartFile video) {
        String authorId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (authorId == null) throw new PostException("Không tìm thấy người dùng");

        try {
            String imageUrl = null;
            String videoUrl = null;

            if (image != null && !image.isEmpty()) {
                imageUrl = cloudinaryConfig.uploadFile(image, "post/images", "image");
            }

            if (video != null && !video.isEmpty()) {
                videoUrl = cloudinaryConfig.uploadFile(video, "post/videos", "video");
            }

            Post post = Post.builder().authorId(authorId).content(req.getContent()).link(req.getLink()).imageURL(imageUrl).videoURL(videoUrl).privacy(req.getPrivacy()).createAt(LocalDate.now()).updateAt(LocalDate.now()).build();
            postRepository.save(post);

            return new PostResponse(true, "Tạo bài viết thành công", convertToDTO(post));
        } catch (Exception e) {
            throw new PostException("Tạo bài viết thất bại: " + e.getMessage());
        }
    }

    private PostDTO convertToDTO(Post post) {
        Optional<UserCache> userCache = userCacheRepository.findById(post.getAuthorId());
        UserCache user = userCache.orElse(null);

        if (user == null) {
            try {
                String url = "http://localhost:8081/user/" + post.getAuthorId();
                ResponseEntity<UserCacheResponse> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, UserCacheResponse.class
                );
                if(response.getStatusCode().is2xxSuccessful() && response.getBody() != null){
                    user = response.getBody().getData();
                    userCacheRepository.save(user);
                }
            } catch (Exception e) {
                throw new PostException("Thất bại trong việc lấy user: "+ e.getMessage());
            }
        }

        return new PostDTO(post.getId(), post.getAuthorId(), user != null ? user.getFName() : "anonymous member", user != null ? user.getLName() : "anonymous member", user != null ? user.getAvatar() : null, post.getContent(), post.getImageURL(), post.getVideoURL(), post.getLink(), post.getPrivacy(), post.getCreateAt(), post.getUpdateAt());
    }

    @Override
    public PostResponse getPostById(String id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostException("Không tìm thấy bài viết"));
        return new PostResponse(true, "Lấy thành công", convertToDTO(post));
    }

    @Override
    public PostResponse updatePost(UpdatePostRequest req, String id, MultipartFile image, MultipartFile video) {
        String authorId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (authorId == null) throw new PostException("Không tìm thấy người dùng");

        Post post = postRepository.findById(id).orElseThrow(() -> new PostException("Không tìm thấy bài viết"));

        if (!post.getAuthorId().equals(authorId)) throw new PostException("Bạn không có quyền chỉnh sửa bài viết này");

        try {
            if (req.getContent() != null) post.setContent(req.getContent());
            if (req.getLink() != null) post.setLink(req.getLink());
            if (req.getPrivacy() != null) post.setPrivacy(req.getPrivacy());
            post.setUpdateAt(LocalDate.now());

            if (image != null && !image.isEmpty()) {
                if (post.getImageURL() != null) {
                    cloudinaryConfig.deleteFileByURL(post.getImageURL(), "image");
                }
                String imageUrl = cloudinaryConfig.uploadFile(image, "post/images", "image");
                post.setImageURL(imageUrl);
            }

            if (video != null && !video.isEmpty()) {
                if (post.getVideoURL() != null) {
                    cloudinaryConfig.deleteFileByURL(post.getVideoURL(), "video");
                }
                String videoUrl = cloudinaryConfig.uploadFile(video, "post/videos", "video");
                post.setVideoURL(videoUrl);
            }

            postRepository.save(post);

            return new PostResponse(true, "Cập nhật thành công", convertToDTO(post));

        } catch (Exception e) {
            throw new PostException("Lỗi khi cập nhật bài viết: " + e.getMessage());
        }
    }

    @Override
    public void deletePost(String id) {
        String authorId = SecurityContextHolder.getContext().getAuthentication().getName();

        Post post = postRepository.findById(id).orElseThrow(() -> new PostException("Không tìm thấy bài viết"));

        if (!post.getAuthorId().equals(authorId)) throw new PostException("Bạn không có quyền xóa bài viết này");

        try {
            if (post.getImageURL() != null) cloudinaryConfig.deleteFileByURL(post.getImageURL(), "image");
            if (post.getVideoURL() != null) cloudinaryConfig.deleteFileByURL(post.getVideoURL(), "video");
        } catch (Exception e) {
            throw new PostException("Lỗi khi xóa bài viết: " + e.getMessage());
        }
    }

    @Override
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll().stream().map(post -> new PostResponse(true, "Lấy thành công", convertToDTO(post))).collect(Collectors.toList());
    }

    @Override
    public ListPostResponse getAllPostsOfUser() {
        String id = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Post> posts = postRepository.findByAuthorId(id);

        List<PostOfUserResponse> data = posts.stream().map(post -> new PostOfUserResponse(post.getId(), post.getContent(), post.getImageURL(), post.getVideoURL(), post.getLink(), post.getPrivacy(), post.getCreateAt(), post.getUpdateAt())).toList();

        return new ListPostResponse(true, "Lấy thành công", data);
    }
}