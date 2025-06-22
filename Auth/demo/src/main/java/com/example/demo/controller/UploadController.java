package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.CloudinaryConfig;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {
    private final CloudinaryConfig cloudinaryConfig;

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,@RequestParam(required = false) String oldUrl) {
        try {
            String newUrl = cloudinaryConfig.uploadFile(file);

            if(oldUrl != null && !oldUrl.isEmpty()){
                cloudinaryConfig.deleteFIle(oldUrl);
            }

            return newUrl;
        } catch (Exception e) {
            throw new RuntimeException("Upload avatar failed: " + e.getMessage());
        }
    }
}
