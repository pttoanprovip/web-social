package com.example.demo.service.Impl;

import com.example.demo.dto.req.CreateLikeRequest;
import com.example.demo.dto.req.UpdateLikeRequest;
import com.example.demo.dto.res.UserCacheRepsonse;
import com.example.demo.dto.res.LikeDTO;
import com.example.demo.dto.res.LikeResponse;
import com.example.demo.enums.Type;
import com.example.demo.event.*;
import com.example.demo.exception.LikeException;
import com.example.demo.model.Like;
import com.example.demo.model.UserCache;
import com.example.demo.repo.LikeRepository;
import com.example.demo.repo.UserCacheRepository;
import com.example.demo.service.LikeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final UserCacheRepository userCacheRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @KafkaListener(topics = "post-create", groupId = "Like", containerFactory = "kafkaListenerContainerFactory")
    public void handlePostCreate(PostCreateEvent event) {
        Like like = Like.builder().postId(event.getId()).build();
    }

    @Override
    @KafkaListener(topics = "post-delete", groupId = "Like", containerFactory = "kafkaListenerContainerFactory")
    public void handlePostDelete(PostDeletedEvent event) {
        String postId = event.getId();
        likeRepository.deleteByPostId(postId);
    }

    @Override
    @KafkaListener(topics = "user-update-name", groupId = "Like", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserUpdateName(UserUpdatedNameEvent event) {
        UserCache user = UserCache.builder().id(event.getId()).fName(event.getFName()).lName(event.getLName())
                .avatar(event.getAvatar()).build();
        userCacheRepository.save(user);
    }

    @Override
    @KafkaListener(topics = "comment-create", groupId = "Like", containerFactory = "kafkaListenerContainerFactory")
    public void handleCommentCreate(CommentCreateEvent event) {
        Like like = Like.builder().commentId(event.getId()).build();
    }

    @Override
    public void handleCommentDelete(CommentDeleteEvent event) {
        String commentId = event.getId();
        likeRepository.deleteById(commentId);
    }

    @Override
    public LikeResponse createLike(CreateLikeRequest req) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new LikeException("Không tìm thấy nguời dùng");

        if (req.getPostId() != null) {
            List<Like> existingPostReaction = likeRepository.findByUserIdAndPostId(userId, req.getPostId());
            if (!existingPostReaction.isEmpty())
                throw new LikeException("Ngươời dùng đã thả cảm xúc ở bài viết này rồi");
        } else {
            List<Like> existingCommentReaction = likeRepository.findByUserIdAndCommentId(userId, req.getCommentId());
            if (!existingCommentReaction.isEmpty())
                throw new LikeException("Ngươời dùng đã thả cảm xúc ở bài viết này rồi");
        }

        Like like =
                Like.builder().userId(userId).postId(req.getPostId()).commentId(req.getCommentId()).type(req.getType())
                        .createAt(LocalDate.now()).build();
        Like savaLike = likeRepository.save(like);

        redisTemplate.opsForValue().set("like:" + savaLike.getId(), convertToDTO(savaLike), 1, TimeUnit.HOURS);

        if (req.getPostId() != null) {
            redisTemplate.delete("reactions:summary:" + req.getPostId());
        }

        if (req.getCommentId() != null) {
            redisTemplate.delete("reactions:comment:summary:" + req.getPostId());
        }

        return new LikeResponse(true, "Tạo thành công", convertToDTO(savaLike));
    }

    @Override
    public void deleteLike(String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Like like = likeRepository.findById(id).orElseThrow(() -> new LikeException("Không tìm thấy cảm xúc"));
        if (!like.getUserId().equals(userId)) throw new LikeException("Bạn không có quyền xóa cảm xúc này");

        if (like.getPostId() != null) {
            redisTemplate.delete("reactions:summary:" + like.getPostId());
        }

        if (like.getCommentId() != null) {
            redisTemplate.delete("reactions:comment:summary:" + like.getPostId());
        }

        likeRepository.deleteById(id);
    }

    @Override
    public LikeResponse updateLike(UpdateLikeRequest req, String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new LikeException("Không tìm thấy người dùng");

        Like like = likeRepository.findById(id).orElseThrow(() -> new LikeException("Không tìm thấy cảm xúc"));
        if (!like.getUserId().equals(userId)) throw new LikeException("Bạn không có quyền chỉnh sửa cảm xúc này");
        Type oldType = like.getType();

        like.setType(req.getType());
        Like saveLike = likeRepository.save(like);

        if (like.getPostId() != null) {
            redisTemplate.delete("reactions:summary:" + like.getPostId());
        }

        if (like.getCommentId() != null) {
            redisTemplate.delete("reactions:comment:summary:" + like.getPostId());
        }

        return new LikeResponse(true, "Chỉnh xữa thành công", convertToDTO(saveLike));
    }

    @Override
    public Map<String, Object> getGroupReactionsByPost(String postId) {
        // 1. Kiểm tra cache
        String redisKey = "reactions:summary:" + postId;

        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
        if (cached != null) return cached;

        // 2. Truy vấn database nếu không có trong cache
        List<Like> likes = likeRepository.findByPostId(postId);
        int total = likes.size();

        // 3. Nhóm reactions theo type
        Map<Type, List<Like>> groupedByType = likes.stream().collect(Collectors.groupingBy(Like::getType));

        // 4. Tính toán thống kê
        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<LikeDTO>> details = new LinkedHashMap<>();
        List<Map<String, Object>> topReactions = new ArrayList<>();
        List<LikeDTO> allReactions = new ArrayList<>();

        // Khởi tạo các loại reaction
        for (Type type : Type.values()) {
            String typeName = type.name();
            List<Like> typeLikes = groupedByType.getOrDefault(type, Collections.emptyList());
            int count = typeLikes.size();

            // Đếm số lượng
            counts.put(typeName, count);

            // Chuyển đổi sang DTO
            List<LikeDTO> dtos = typeLikes.stream().map(this::convertToDTO).collect(Collectors.toList());

            details.put(typeName, dtos);
            allReactions.addAll(dtos);
        }

        // Tạo top reactions (sắp xếp giảm dần)
        counts.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(3) // Chỉ lấy top 3
                .forEach(entry -> {
                    Map<String, Object> top = new HashMap<>();
                    top.put("type", entry.getKey());
                    top.put("count", entry.getValue());
                    topReactions.add(top);
                });

        // 5. Xây dựng kết quả cuối cùng
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("postId", postId);
        result.put("total", total);
        result.put("counts", counts);
        result.put("details", details);
        result.put("topReactions", topReactions);
        result.put("allReactions", allReactions);

        // 6. Lưu vào Redis cache
        redisTemplate.opsForValue().set(redisKey, result);

        return result;
    }

    @Override
    public Map<String, Object> getGroupReactionsByComment(String commentId) {
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue()
                .get("reactions:comment:summary: " + commentId);
        if (cached != null) return cached;

        List<Like> likes = likeRepository.findByCommentId(commentId);
        int total = likes.size();

        Map<Type, List<Like>> groupedByType = likes.stream().collect(Collectors.groupingBy(Like::getType));

        Map<String, Integer> counts = new HashMap<>();
        Map<String, List<LikeDTO>> details = new LinkedHashMap<>();
        List<Map<String, Object>> topReactions = new ArrayList<>();
        List<LikeDTO> allReactions = new ArrayList<>();

        for (Type type : Type.values()) {
            String typeName = type.name();
            List<Like> typeLikes = groupedByType.getOrDefault(type, Collections.emptyList());
            int count = typeLikes.size();

            counts.put(typeName, count);
            List<LikeDTO> dtos = typeLikes.stream().map(this::convertToDTO).collect(Collectors.toList());

            details.put(typeName, dtos);
            allReactions.addAll(dtos);
        }

        counts.entrySet().stream().filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .reversed()).limit(3)
                .forEach(entry -> {
                    Map<String, Object> top = new HashMap<>();
                    top.put("type", entry.getKey());
                    top.put("count", entry.getValue());
                    topReactions.add(top);
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("commentId", commentId);
        result.put("total", total);
        result.put("counts", counts);
        result.put("details", details);
        result.put("topReactions", topReactions);
        result.put("allReactions", allReactions);

        redisTemplate.opsForValue().set("reactions:comment:summary:" + commentId, result);

        return result;
    }


    @Override
    public LikeResponse getHistoryReactionOfUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new LikeException("Không tìm thấy người dùng");

        List<LikeDTO> cacheLike = (List<LikeDTO>) redisTemplate.opsForValue().get("like:user:" + userId);
        if (cacheLike != null) {
            return new LikeResponse(true, "Lấy thành công từ cache", cacheLike);
        } else {
            List<Like> historyLikes = likeRepository.findByUserId(userId);
            List<LikeDTO> dto = historyLikes.stream().map(this::convertToDTO).collect(Collectors.toList());

            redisTemplate.opsForValue().set("like:user:" + userId, dto, 1, TimeUnit.HOURS);
            return new LikeResponse(true, "Lấy thành công", dto);
        }
    }


    public LikeDTO convertToDTO(Like like) {
        UserCache user = null;
        Object rawUser = redisTemplate.opsForValue().get("user:cache:" + like.getUserId());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        if (rawUser != null) {
            try {
                user = objectMapper.convertValue(rawUser, UserCache.class);
            } catch (Exception e) {
                // Log lỗi để debug
                System.err.println("Lỗi khi chuyển đổi từ Redis: " + e.getMessage());
            }
        }

        if (user == null) {
            Optional<UserCache> userCache = userCacheRepository.findById(like.getUserId());
            user = userCache.orElse(null);

            if (user == null) {
                try {
                    String url = "http://localhost:8081/user/" + like.getUserId();
                    ResponseEntity<UserCacheRepsonse> response = restTemplate.exchange(url, HttpMethod.GET, null,
                            new ParameterizedTypeReference<UserCacheRepsonse>() {
                            });
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        user = response.getBody().getData();
                        if (user != null) {
                            userCacheRepository.save(user);
                            redisTemplate.opsForValue().set("user:cache:" + like.getUserId(), user, 1, TimeUnit.HOURS);
                        }
                    }
                } catch (Exception e) {
                    throw new LikeException("Thất bại trong việc lấy user: " + e.getMessage());
                }
            } else {
                redisTemplate.opsForValue().set("user:cache:" + like.getUserId(), user, 1, TimeUnit.HOURS);
            }
        }

        return new LikeDTO(like.getId(), like.getUserId(), like.getPostId(),
                like.getCommentId(), like.getType(),
                user != null ? user.getAvatar() : null,
                user != null ? user.getFName() : null,
                user != null ? user.getLName() : null,
                like.getCreateAt());
    }
}

