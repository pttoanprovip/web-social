package com.example.demo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrivateProfile {

    private boolean success;
    private String message;
    private UserPublicDTO user;
    private String email;
    private String phone;
}
