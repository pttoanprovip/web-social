package com.example.demo.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {
    private String email;
    private String phone;
    private String password;
}
