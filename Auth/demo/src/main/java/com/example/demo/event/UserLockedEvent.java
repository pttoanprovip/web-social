package com.example.demo.event;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLockedEvent {
    private String id;
    private LocalDate lockedUntil;
}
