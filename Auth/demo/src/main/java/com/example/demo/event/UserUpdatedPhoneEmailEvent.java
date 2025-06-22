package com.example.demo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedPhoneEmailEvent {
    private String id;
    private String email;
    private String phone;
    private LocalDate updateAt;
}
