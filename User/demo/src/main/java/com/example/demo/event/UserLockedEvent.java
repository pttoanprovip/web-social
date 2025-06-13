package com.example.demo.event;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import lombok.Data;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLockedEvent {
    private String id;
    private LocalDate lockedUntil;
}
