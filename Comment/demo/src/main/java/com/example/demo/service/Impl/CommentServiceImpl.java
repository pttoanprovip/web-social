package com.example.demo.service.Impl;

import com.example.demo.config.CloudinaryConfig;
import com.example.demo.dto.req.CreateCommentRequest;
import com.example.demo.dto.req.ReplyCommentRequest;
import com.example.demo.dto.req.UpdateCommentRequest;
import com.example.demo.dto.res.CommentDTO;
import com.example.demo.dto.res.CommentResponse;
import com.example.demo.dto.res.UserCacheResponse;
import com.example.demo.event.PostCreateEvent;
import com.example.demo.event.PostDeletedEvent;
import com.example.demo.event.UserUpdatedNameEvent;
import com.example.demo.exception.CommentException;
import com.example.demo.model.Comment;
import com.example.demo.model.UserCache;
import com.example.demo.repo.CommentRepository;
import com.example.demo.repo.UserCacheRepository;
import com.example.demo.service.CommentService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserCacheRepository userCacheRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CloudinaryConfig cloudinary;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @KafkaListener(topics = "post-create", groupId = "Comment", containerFactory = "kafkaListenerContainerFactory")
    public void handlePostCreate(PostCreateEvent event) {
        Comment comment = Comment.builder().postId(event.getId()).build();
    }

    @Override
    @KafkaListener(topics = "post-delete", groupId = "Comment", containerFactory = "kafkaListenerContainerFactory")
    public void handlePostDelete(PostDeletedEvent event) {
        String postId = event.getId();
        commentRepository.deletePostById(postId);
    }

    @Override
    @KafkaListener(topics = "user-update_name", groupId = "Comment", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserUpdateName(UserUpdatedNameEvent event) {
        UserCache userCache = UserCache.builder().id(event.getId()).fName(event.getFName()).lName(event.getLName()).avatar(event.getAvatar()).build();
        userCacheRepository.save(userCache);
    }

    @Override
    public CommentResponse createComment(CreateCommentRequest req, MultipartFile image, MultipartFile video) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) {
            throw new RuntimeException("Không tìm thấy người dùng");
        }

        try {
            String imageUrl = null;
            String videoUrl = null;

            if (image != null && !image.isEmpty()) {
                imageUrl = cloudinary.uploadFile(image, "comment/images", "image");
            }
            if (video != null && !video.isEmpty()) {
                videoUrl = cloudinary.uploadFile(video, "comment/videos", "video");
            }

            Comment comment = Comment.builder().postId(req.getPostId()).userId(userId).content(req.getContent()).imageURL(imageUrl).videoURL(videoUrl).createAt(LocalDate.now()).build();

            commentRepository.save(comment);

            return new CommentResponse(true, "Tạo bình luận thành công", convertToDTO(comment));
        } catch (Exception e) {
            throw new CommentException("Lỗi khi tạo bình luận: " + e.getMessage());
        }
    }

    @Override
    public CommentResponse updateComment(UpdateCommentRequest req, String id, MultipartFile image, MultipartFile video) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new CommentException("Không tìm thấy người dùng");

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentException("Không tìm thấy bình luận này"));
        if (!comment.getUserId().equals(userId))
            throw new CommentException("Bạn không có quyền chỉnh sửa bình luận này");

        try {
            if (req.getContent() != null) {
                comment.setContent(req.getContent());
            }

            if (image != null && !image.isEmpty()) {
                if (comment.getImageURL() != null) {
                    cloudinary.deleteFileByURL(comment.getImageURL(), "image");
                }
                String imageUrl = cloudinary.uploadFile(image, "post/images", "image");
                comment.setImageURL(imageUrl);
            }

            if (video != null && !video.isEmpty()) {
                if (comment.getVideoURL() != null) {
                    cloudinary.deleteFileByURL(comment.getVideoURL(), "video");
                }
                String videoUrl = cloudinary.uploadFile(video, "post/videos", "video");
                comment.setImageURL(videoUrl);
            }

            commentRepository.save(comment);

            return new CommentResponse(true, "Cập nhật bình luận thành công", convertToDTO(comment));
        } catch (Exception e) {
            throw new CommentException("Lỗi khi cập nhật bình luận: " + e.getMessage());
        }
    }

    @Override
    public void deleteComment(String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new CommentException("Không tìm thấy người dùng");

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentException("Không tìm thấy bình luận"));
        if (!comment.getUserId().equals(userId)) throw new CommentException("Bạn không có quyền xóa bình luận này");

        try {
            if (comment.getImageURL() != null)
                cloudinary.deleteFileByURL(comment.getImageURL(), "image");
            if (comment.getVideoURL() != null)
                cloudinary.deleteFileByURL(comment.getVideoURL(), "video");

            commentRepository.deleteById(id);
        } catch (Exception e) {
            throw new CommentException("Lỗi khi xóa bình luận: " + e.getMessage());
        }
    }

    @Override
    public CommentResponse getCommentByPost(String postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        if (comments.isEmpty()) throw new CommentException("Bài viết chưa có bình luận nào");

        List<CommentDTO> commentDTOs = comments.stream().map(this::convertToDTO).toList();

        return new CommentResponse(true, "Lấy bình luận thành công", commentDTOs);
    }

    @Override
    public CommentResponse replyComment(ReplyCommentRequest req, String parentCommentId, MultipartFile image, MultipartFile video) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new CommentException("Không tìm thấy người dùng");

        commentRepository.findById(parentCommentId).orElseThrow(() -> new CommentException("Không tìm thấy bình luận gốc"));

        try {
            String imageUrl = null;
            String videoUrl = null;

            if (image != null && !image.isEmpty()) {
                imageUrl = cloudinary.uploadFile(image, "comment/images", "image");
            }
            if (video != null && !video.isEmpty()) {
                videoUrl = cloudinary.uploadFile(video, "comment/videos", "video");
            }

            Comment comment = Comment.builder()
                    .postId(req.getPostId())
                    .userId(userId)
                    .content(req.getContent())
                    .imageURL(imageUrl)
                    .videoURL(videoUrl)
                    .createAt(LocalDate.now())
                    .parentCommentId(parentCommentId)
                    .build();

            commentRepository.save(comment);

            return new CommentResponse(true, "Tạo bình luận thành công", convertToDTO(comment));
        } catch (Exception e) {
            throw new CommentException("Lỗi khi tạo bình luận: " + e.getMessage());
        }
    }

    public CommentDTO convertToDTO(Comment comment) {
        Optional<UserCache> userCache = userCacheRepository.findById(comment.getUserId());
        UserCache user = userCache.orElse(null);

        if (user == null) {
            try {
                String url = "http://localhost:8081/user/" + comment.getUserId();
                ResponseEntity<UserCacheResponse> res = restTemplate.exchange(url, HttpMethod.GET, null, UserCacheResponse.class);

                if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                    user = res.getBody().getData();
                    userCacheRepository.save(user);
                }
            } catch (Exception e) {
                System.out.println("Không thể lấy thông tin userId: " + comment.getUserId());
                return new CommentDTO(comment.getId(), comment.getPostId(), comment.getUserId(), comment.getContent(), comment.getImageURL(), comment.getVideoURL(), comment.getCreateAt(), null, null, null, comment.getParentCommentId());
            }
        }

        return CommentDTO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .imageURL(comment.getImageURL())
                .videoURL(comment.getVideoURL())
                .createAt(comment.getCreateAt())
                .avatar(user != null ? user.getAvatar() : null)
                .fname(user != null ? user.getFName() : null)
                .lname(user != null ? user.getLName() : null)
                .parentCommentId(comment.getParentCommentId())
                .build();
    }
}
