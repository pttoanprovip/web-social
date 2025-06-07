package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.CloudinaryConfig;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {
    private final CloudinaryConfig cloudinaryConfig;

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            return cloudinaryConfig.uploadFile(file);
        } catch (Exception e) {
            throw new RuntimeException("Upload avatar failed: " + e.getMessage());
        }
    }
}
