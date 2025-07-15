package com.example.demo.service.Impl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.example.demo.event.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.demo.dto.req.ChangePasswordRequest;
import com.example.demo.dto.req.LoginRequest;
import com.example.demo.dto.req.RegisterRequest;
import com.example.demo.dto.res.LoginResponse;
import com.example.demo.dto.res.RegisterResponse;
import com.example.demo.exception.AuthException;
import com.example.demo.model.Auth;
import com.example.demo.repo.AuthRepository;
import com.example.demo.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender javaMailSender;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGN_KEY;

    @NonFinal
    @Value("${server.port}")
    protected String PORT;

    @Override
    public LoginResponse login(LoginRequest req) {

        String identifier = (req.getEmail() != null && !req.getEmail().isEmpty()) ? req.getEmail() : (req.getPhone() != null && !req.getPhone().isEmpty()) ? req.getPhone() : null;

        if (identifier == null) {
            throw new AuthException("Vui lòng cung cấp email hoặc số điện thoại");
        }

        Auth auth = (req.getEmail() != null) ? authRepository.findByEmail(req.getEmail()) : authRepository.findByPhone(req.getPhone());

        if (auth == null || !passwordEncoder.matches(req.getPassword(), auth.getPassword())) {
            throw new AuthException("Thông tin đăng nhập không hợp lệ");
        }

        if (auth.getLocked() != null && auth.getLocked()) {
            if (auth.getLockedUntil() != null && auth.getLockedUntil().isAfter(LocalDate.now())) {
                throw new AuthException("Tài khoản đã bị khóa");
            } else if (auth.getLockedUntil() != null && !auth.getLockedUntil().isAfter(LocalDate.now())) {
                auth.setLocked(false);
                auth.setLockedUntil(null);
                authRepository.save(auth);
            }
        }

        String token = generateToken(auth);
        redisTemplate.opsForValue().set("token:" + token, auth.getId(), 30, TimeUnit.MINUTES);

        return new LoginResponse(token);
    }

    @Override
    public RegisterResponse register(RegisterRequest req) {
        if ((req.getEmail() != null && authRepository.existsByEmail(req.getEmail())
                || (req.getPhone() != null && authRepository.existsByPhone(req.getPhone())))) {
            throw new AuthException("Email hoặc số điện thoại đã được sử dụng");
        }

        Auth auth = new Auth();
        auth.setEmail(req.getEmail());
        auth.setPhone(req.getPhone());
        auth.setPassword(passwordEncoder.encode(req.getPassword()));

        Auth saveAuth = authRepository.save(auth);
        String token = generateToken(saveAuth);
        redisTemplate.opsForValue().set("token:" + token, auth.getId(), 30, TimeUnit.MINUTES);

        UserRegisteredEvent event = new UserRegisteredEvent(saveAuth.getId(), req.getEmail(), req.getPhone(), req.getFName(), req.getLName(), req.getDob(), req.getGender().name(), req.getAvatar(), LocalDate.now());

        // Gửi sự kiện kafka đăng ký
        kafkaTemplate.send("user-registered", event);

        return new RegisterResponse(saveAuth.getId(), token);
    }

    @Override
    public void logout(String token) {
        try {
            if (token == null || token.isEmpty()) {
                throw new AuthException("Token không hợp lệ");
            }
            DecodedJWT decodedJWT = verifyToken(token);
            Date expiresAt = decodedJWT.getExpiresAt();
            long ttl = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;

            if (ttl > 0) {
                redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw new AuthException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    public String generateToken(Auth auth) {
        return JWT.create().withSubject(auth.getId()).withClaim("id", auth.getId()).withClaim("email", auth.getEmail()).withClaim("phone", auth.getPhone()).withIssuedAt(new Date()).withExpiresAt(Date.from(Instant.now().plusSeconds(3600))).sign(Algorithm.HMAC256(SIGN_KEY));
    }

    public DecodedJWT verifyToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(SIGN_KEY)).build().verify(token);
        } catch (Exception e) {
            throw new AuthException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    @Override
    public void changePassword(String id, ChangePasswordRequest req) {
        Auth auth = authRepository.findById(id).orElseThrow(() -> new AuthException("Người dùng không tồn tại"));
        if (!passwordEncoder.matches(req.getOldPassword(), auth.getPassword())) {
            throw new AuthException("Mật khẩu cũ không đúng");
        }
        auth.setPassword(passwordEncoder.encode(req.getNewPassword()));
        authRepository.save(auth);
    }

    @Override
    public void changeEmail(String id, String newEmail) {
        if (authRepository.existsByEmail(newEmail)) {
            throw new AuthException("Email đã được sử dụng");
        }
        Auth auth = authRepository.findById(id).orElseThrow(() -> new AuthException("Người dùng không tồn tại"));
        auth.setEmail(newEmail);
        authRepository.save(auth);

        kafkaTemplate.send("user_phone_email-change", new UserUpdatedPhoneEmailEvent(id, newEmail, auth.getPhone(), LocalDate.now()));
    }

    @Override
    public void changePhone(String id, String newPhone) {
        if (authRepository.existsByPhone(newPhone)) {
            throw new AuthException("Số điện thoại đã được sử dụng");
        }
        Auth auth = authRepository.findById(id).orElseThrow(() -> new AuthException("Người dùng không tồn tại"));
        auth.setPhone(newPhone);
        authRepository.save(auth);

        kafkaTemplate.send("user_phone_email-change", new UserUpdatedPhoneEmailEvent(id, auth.getEmail(), newPhone, LocalDate.now()));
    }

    @Override
    @KafkaListener(topics = "user-deleted", groupId = "Auth", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserDelete(UserDeleteEvent event) {
        authRepository.deleteById(event.getId());
    }

    @Override
    @KafkaListener(topics = "user-locked", groupId = "Auth", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserLocked(UserLockedEvent event) {
        Auth auth = authRepository.findById(event.getId()).orElse(null);
        if (auth != null) {
            auth.setLocked(true);
            auth.setLockedUntil(event.getLockedUntil());
            authRepository.save(auth);
        }
    }

    @Override
    @KafkaListener(topics = "user-unlocked", groupId = "Auth", containerFactory = "kafkaListenerContainerFactory")
    public void handleUserUnlocked(UserUnlockedEvent event) {
        Auth auth = authRepository.findById(event.getId()).orElse(null);
        if (auth != null) {
            auth.setLocked(false);
            auth.setLockedUntil(null);
            authRepository.save(auth);
        }
    }

    @Override
    public void requestUnlockByEmail(String email) {
        Auth auth = authRepository.findByEmail(email);

        if (auth == null) {
            throw new AuthException("Email không tồn tại");
        }

        if (!auth.getLocked()) {
            throw new AuthException("Tài khoản không bị khóa");
        }

        String unlockToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("unlockToken:" + unlockToken, auth.getId(), 24, TimeUnit.HOURS);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Yêu cầu mở khóa tài khoản");
        message.setText("Vui lòng nhấp vào link sau để mở khóa tài khoản: " + "http://localhost:" + PORT + "/auth/unlock?token=" + unlockToken);
        javaMailSender.send(message);
    }

    @Override
    public void unlockByToken(String token) {
        String userId = redisTemplate.opsForValue().get("unlockToken:" + token).toString();
        if (userId == null) {
            throw new AuthException("Mã thông báo không hợp lệ hoặc đã hết hạn");
        }

        Auth auth = authRepository.findById(userId).orElseThrow(() -> new AuthException("Người dùng không tồn tại"));
        auth.setLocked(false);
        auth.setLockedUntil(null);

        authRepository.save(auth);

        redisTemplate.delete("unlockToken:" + token);

        kafkaTemplate.send("user-unlocked", new UserUnlockedEvent(userId));
    }
}
