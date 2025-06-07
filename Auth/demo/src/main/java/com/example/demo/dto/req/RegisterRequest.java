package com.example.demo.dto.req;

import java.time.LocalDate;

import com.example.demo.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String phone;
    private String password;
    private String fName;
    private String lName;
    private LocalDate dob;
    private Gender gender;
    private String avatar;
}
