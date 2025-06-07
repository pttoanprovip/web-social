package com.example.demo.dto.req;

import java.time.LocalDate;

import com.example.demo.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateUserRequest {
    private String fName;
    private String lName;
    private Gender gender;
    private String bio;
    private LocalDate dob;
}
