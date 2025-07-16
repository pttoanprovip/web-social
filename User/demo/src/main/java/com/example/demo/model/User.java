package com.example.demo.model;

import java.time.LocalDate;

import com.example.demo.enums.Gender;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;
    private String email;
    private String phone;
    private String fName;
    private String lName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private Gender gender;
    private String avatar;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;
    private String bio;
    private String background;
    private Boolean isLocked = false;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lockedUntil;
}
