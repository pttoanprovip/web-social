package com.example.demo.service.Impl;

import com.example.demo.dto.req.CreateLikeRequest;
import com.example.demo.dto.req.UpdateLikeRequest;
import com.example.demo.dto.res.UserCacheRepsone;
import com.example.demo.dto.res.LikeDTO;
import com.example.demo.dto.res.LikeResponse;
import com.example.demo.enums.Type;
import com.example.demo.event.PostCreateEvent;
import com.example.demo.event.PostDeletedEvent;
import com.example.demo.event.UserUpdatedNameEvent;
import com.example.demo.exception.LikeException;
import com.example.demo.model.Like;
import com.example.demo.model.UserCache;
import com.example.demo.repo.LikeRepository;
import com.example.demo.repo.UserCacheRepository;
import com.example.demo.service.LikeService;
import lombok.RequiredArgsConstructor;
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
        UserCache user = UserCache.builder().id(event.getId()).fName(event.getFName()).lName(event.getLName()).avatar(event.getAvatar()).build();
        userCacheRepository.save(user);
    }

    @Override
    public LikeResponse createLike(CreateLikeRequest req) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new LikeException("Không tìm thấy nguời dùng");

        Optional<Like> existingLike = likeRepository.findByUserIdAndPostId(userId, req.getPostId());
        if (existingLike.isPresent()) throw new LikeException("Người dùng đã thả cảm xúc vào bài viết này rồi");

        Like like = Like.builder().userId(userId).postId(req.getPostId()).type(req.getType()).createAt(LocalDate.now()).build();
        Like savaLike = likeRepository.save(like);

        redisTemplate.delete("reactions:summary:" + req.getPostId());

        return new LikeResponse(true, "Tạo thành công", convertToDTO(savaLike));
    }

    @Override
    public void deleteLike(String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Like like = likeRepository.findById(id).orElseThrow(() -> new LikeException("Không tìm thấy bài viết"));
        if (!like.getUserId().equals(userId)) throw new LikeException("Bạn không có quyền xóa cảm xúc này");

        likeRepository.deleteById(id);
        redisTemplate.delete("reactions:summary:" + like.getPostId());
    }

    @Override
    public LikeResponse updateLike(UpdateLikeRequest req, String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new LikeException("Không tìm thấy người dùng");

        Like like = likeRepository.findById(id).orElseThrow(() -> new LikeException("Không tìm thấy bài viết"));
        if (!like.getUserId().equals(userId)) throw new LikeException("Bạn không có quyền chỉnh sửa cảm xúc này");
        Type oldType = like.getType();

        like.setType(req.getType());
        Like saveLike = likeRepository.save(like);

        redisTemplate.delete("reactions:summary:" + like.getPostId());

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
        Map<Type, List<Like>> groupedByType = likes.stream()
                .collect(Collectors.groupingBy(Like::getType));

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
            List<LikeDTO> dtos = typeLikes.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            details.put(typeName, dtos);
            allReactions.addAll(dtos);
        }

        // Tạo top reactions (sắp xếp giảm dần)
        counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3) // Chỉ lấy top 3
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
    public LikeResponse getHistoryReactionOfUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userId == null) throw new LikeException("Không tìm thấy người dùng");

        List<Like> historyLikes = likeRepository.findByUserId(userId);
        List<LikeDTO> dto = historyLikes.stream().map(this::convertToDTO).collect(Collectors.toList());

        return new LikeResponse(true, "Lấy thành công", dto);
    }


    public LikeDTO convertToDTO(Like like) {
        Optional<UserCache> userCache = userCacheRepository.findById(like.getUserId());
        UserCache user = userCache.orElse(null);

        if (user == null) {
            try {
                String url = "http://localhost:8081/user/" + like.getUserId();
                ResponseEntity<UserCacheRepsone> response = restTemplate.exchange(url, HttpMethod.GET, null, UserCacheRepsone.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    user = response.getBody().data;
                    userCacheRepository.save(user);
                }
            } catch (Exception e) {
                System.out.println("Không thể lấy thông tin userId: " + like.getUserId());
                return new LikeDTO(like.getId(), like.getUserId(), like.getPostId(), like.getType(), null, null, null, like.getCreateAt());
            }
        }

        return new LikeDTO(like.getId(), like.getUserId(), like.getPostId(), like.getType(), user != null ? user.getAvatar() : null, user != null ? user.getFName() : null, user != null ? user.getLName() : null, like.getCreateAt());
    }
}

