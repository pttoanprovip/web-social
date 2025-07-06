package com.example.demo.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.exception.CommentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }

    public String uploadFile(MultipartFile file, String folder, String resourceType) throws Exception {
        Cloudinary cloudinary = cloudinary();
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder, "resource_type", resourceType));
        return uploadResult.get("url").toString();
    }

    public void deleteFileByURL(String fileUrl, String resourceType ) throws Exception{
        String publicId = extractPublicId(fileUrl);
        deleteFile(publicId, resourceType);
    }

    public void deleteFile(String publicId, String resourceType) throws Exception{
        Cloudinary cloudinary = cloudinary();
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", resourceType,
                "invalidate", true
        );
        cloudinary.uploader().destroy(publicId, params);
    }

    private String extractPublicId(String fileUrl) {
        String[] parts = fileUrl.split("/upload/");
        if (parts.length < 2) throw new CommentException("URL không tồn tại");

        String path = parts[1];
        int dotIndex = path.lastIndexOf(".");
        if (dotIndex != -1) {
            path = path.substring(0, dotIndex);
        }
        return path;
    }
}
