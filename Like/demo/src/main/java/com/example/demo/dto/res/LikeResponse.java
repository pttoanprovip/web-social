package com.example.demo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.spec.OAEPParameterSpec;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeResponse {
    private boolean success;
    private String message;
    public Object data;
}
