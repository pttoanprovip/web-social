package com.example.demo.service;

import java.util.List;

import com.example.demo.event.UserUpdatedPhoneEmailEvent;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.req.UpdateUserRequest;
import com.example.demo.dto.res.UserPrivateProfile;
import com.example.demo.dto.res.UserResponse;
import com.example.demo.event.UserRegisteredEvent;
import com.example.demo.event.UserUnlockedEvent;

public interface UserService {

    void handleUserRegister(UserRegisteredEvent event);

    UserResponse update(String id, UpdateUserRequest req);

    void delete(String id);

    void lockProfile(String id);

    void unlockProfile(String id);

    void autoUnlockExpiredAccounts();

    void handleUserUnlocked(UserUnlockedEvent event);

    void handleUserUpdate(UserUpdatedPhoneEmailEvent event);

    UserResponse getById(String id);

    List<UserResponse> getAll();

    List<UserResponse> findByFirstName(String fName);

    List<UserResponse> findByLastName(String lName);

    UserResponse updateAvatar(String id, MultipartFile file);

    UserResponse updateBackground(String id, MultipartFile file);

    UserPrivateProfile getMyInfo();
}
