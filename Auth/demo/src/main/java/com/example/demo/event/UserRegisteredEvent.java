package com.example.demo.event;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent {
    private String id;
    private String email;
    private String phone;
    private String fName;
    private String lName;
    private LocalDate dob;
    private String gender;
    private String avatar;
    private LocalDate createdAt = LocalDate.now();
}
