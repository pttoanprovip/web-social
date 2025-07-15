package com.example.demo.service.Impl;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.demo.config.RedisConfig;
import com.example.demo.event.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.CloudinaryConfig;
import com.example.demo.dto.req.UpdateUserRequest;
import com.example.demo.dto.res.UserPrivateProfile;
import com.example.demo.dto.res.UserPublicDTO;
import com.example.demo.dto.res.UserResponse;
import com.example.demo.enums.Gender;
import com.example.demo.exception.UserException;
import com.example.demo.model.User;
import com.example.demo.repo.UserRepository;
import com.example.demo.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CloudinaryConfig cloudinaryConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @KafkaListener(topics = "user-registered", groupId = "User", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserRegister(UserRegisteredEvent event) {
        System.out.println("Received event from Kafka: " + event);
        try {
            User user = User.builder()
                    .id(event.getId())
                    .fName(event.getFName())
                    .lName(event.getLName())
                    .dob(event.getDob())
                    .gender(event.getGender() != null ? Gender.valueOf(event.getGender()) : null)
                    .avatar(event.getAvatar())
                    .createdAt(event.getCreatedAt())
                    .email(event.getEmail())
                    .phone(event.getPhone())
                    .build();
            if (!userRepository.existsById(user.getId())) {
                userRepository.save(user);
            }

            redisTemplate.opsForValue().set("user:" + user.getId(), user, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new UserException(e.getMessage());
        }
    }

    @Override
    public UserResponse update(String id, UpdateUserRequest req) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));

        if (req.getFName() != null)
            user.setFName(req.getFName());
        if (req.getLName() != null)
            user.setLName(req.getLName());
        if (req.getGender() != null)
            user.setGender(req.getGender());
        if (req.getBio() != null)
            user.setBio(req.getBio());
        if (req.getDob() != null)
            user.setDob(req.getDob());

        User savaUser = userRepository.save(user);
        redisTemplate.opsForValue().set("user:" + id, savaUser, 10, TimeUnit.MINUTES);

        UserUpdatedNameEvent event = new UserUpdatedNameEvent(
                savaUser.getId(),
                savaUser.getFName(),
                savaUser.getLName(),
                savaUser.getAvatar()
        );

        kafkaTemplate.send("user-updated_name", event);
        return new UserResponse(true, "Cập nhật thành công", convertTPublicDTO(savaUser));
    }

    @Override
    public void delete(String id) {
        userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));
        userRepository.deleteById(id);

        redisTemplate.delete("user:" + id);
        // Gửi sự kiện kafka xóa người dùng đến message
        kafkaTemplate.send("user-deleted", new UserDeleteEvent(id));
    }

    @Override
    public UserResponse getById(String id) {
        User user = (User) redisTemplate.opsForValue().get("user:" + id);
        if(user == null){
            user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));
            redisTemplate.opsForValue().set("user:" + id, user, 10, TimeUnit.MINUTES);
        }
        return new UserResponse(true, "Lấy thành công", convertTPublicDTO(user));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(true, "Lấy thành công", convertTPublicDTO(user)))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> findByFirstName(String fName) {
        return userRepository.findByFirstName(fName).stream()
                .map(user -> new UserResponse(true, "Lấy thành công", convertTPublicDTO(user)))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> findByLastName(String lName) {
        return userRepository.findByLastName(lName).stream()
                .map(user -> new UserResponse(true, "Lấy thành công", convertTPublicDTO(user)))
                .collect(Collectors.toList());
    }

    @Override
    public UserPrivateProfile getMyInfo() {
        String id = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));
        return new UserPrivateProfile(true, "Lấy thành công", convertTPublicDTO(user), user.getEmail(),
                user.getPhone());
    }

    private UserPublicDTO convertTPublicDTO(User user) {
        return new UserPublicDTO(
                user.getId(),
                user.getFName(),
                user.getLName(),
                user.getAvatar(),
                user.getBio(),
                user.getBackground(),
                user.getDob(),
                user.getGender());
    }

    @Override
    public UserResponse updateAvatar(String id, MultipartFile file) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));
        try {
            String avatar = cloudinaryConfig.uploadFile(file);
            user.setAvatar(avatar);
        } catch (Exception e) {
            throw new UserException("Cập nhật ảnh đại diện thất bại");
        }
        User savaUser = userRepository.save(user);
        UserUpdatedNameEvent event = new UserUpdatedNameEvent(
                savaUser.getId(),
                savaUser.getFName(),
                savaUser.getLName(),
                savaUser.getAvatar()
        );
        kafkaTemplate.send("user-updated_name", event);

        return new UserResponse(true, "Cập nhật thành công", convertTPublicDTO(userRepository.save(user)));
    }

    @Override
    public UserResponse updateBackground(String id, MultipartFile file) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));
        try {
            String background = cloudinaryConfig.uploadFile(file);
            user.setBackground(background);
        } catch (Exception e) {
            throw new UserException("Cập nhật ảnh nền thất bại");
        }
        return new UserResponse(true, "Cập nhật thành công", convertTPublicDTO(userRepository.save(user)));
    }

    @Override
    public void lockProfile(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));
        // Tính ngày mở khóa tài khoản là 90 ngày
        LocalDate unlockDate = LocalDate.now().plusDays(90);

        user.setIsLocked(true);
        user.setLockedUntil(unlockDate);
        userRepository.save(user);

        // gửi sự kiện kafka khóa đến message
        kafkaTemplate.send("user-locked", new UserLockedEvent(id, unlockDate));
    }

    @Override
    public void unlockProfile(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User không tồn tại"));

        user.setIsLocked(false);
        user.setLockedUntil(null);

        userRepository.save(user);

        // gửi sự kiện kafka mở khóa đến message
        kafkaTemplate.send("user-unlocked", new UserUnlockedEvent(id));
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void autoUnlockExpiredAccounts() {
        LocalDate today = LocalDate.now();
        List<User> expiredAccounts = userRepository.findByLockedTrueAndLockedUntilBefore(today);
        for (User user : expiredAccounts) {
            user.setIsLocked(false);
            user.setLockedUntil(null);

            userRepository.save(user);

            // gửi sự kiện kafka mở khóa đến message
            kafkaTemplate.send("user-unlocked", new UserUnlockedEvent(user.getId()));
        }
    }

    @Override
    @KafkaListener(topics = "user-unlocked", groupId = "User", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserUnlocked(UserUnlockedEvent event) {
        User user = userRepository.findById(event.getId()).orElse(null);
        if(user !=null){
            user.setIsLocked(false);
            user.setLockedUntil(null);

            userRepository.save(user);
        }
    }

    @Override
    @KafkaListener(topics = "user_phone_email-change")
    public void handleUserUpdate(UserUpdatedPhoneEmailEvent event) {
        User user = userRepository.findById(event.getId()).orElse(null);
        if(user != null){
            if(event.getEmail() != null){
                user.setEmail(event.getEmail());
            }
            if(event.getPhone() != null){
                user.setPhone(event.getPhone());
            }
            userRepository.save(user);
        }
    }

}
