package com.example.demo.dto.res;

import java.time.LocalDate;

import com.example.demo.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPublicDTO {
    private String id;
    private String fName;
    private String lName;
    private String avatar;
    private String bio;
    private String background;
    private LocalDate dob;
    private Gender gender;
}
