package com.example.demo.dto.res;

import com.example.demo.model.UserCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheResponse {
    private boolean success;
    private String message;
    private UserCache data;
}
