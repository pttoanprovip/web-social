package com.example.demo.dto.res;

import com.example.demo.model.UserCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheRepsone {
    private boolean success;
    private String message;
    public UserCache data;
}
