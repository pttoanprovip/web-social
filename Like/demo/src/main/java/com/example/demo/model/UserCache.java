package com.example.demo.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash(value = "user-cache", timeToLive = 86400)
public class UserCache {
    @Id
    private String id; // id của author

    private String fName;
    private String lName;
    private String avatar;
}
