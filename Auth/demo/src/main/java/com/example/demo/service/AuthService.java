package com.example.demo.service;

import com.example.demo.dto.req.ChangePasswordRequest;
import com.example.demo.dto.req.LoginRequest;
import com.example.demo.dto.req.RegisterRequest;
import com.example.demo.dto.res.LoginResponse;
import com.example.demo.dto.res.RegisterResponse;
import com.example.demo.event.UserDeleteEvent;

public interface AuthService {
    LoginResponse login(LoginRequest req);

    RegisterResponse register(RegisterRequest req);

    void logout(String token);

    void changePassword(String id, ChangePasswordRequest req);

    void changeEmail(String id, String newEmail);

    void changePhone(String id, String newPhone);

    void handleUserDelete(UserDeleteEvent event);
}
