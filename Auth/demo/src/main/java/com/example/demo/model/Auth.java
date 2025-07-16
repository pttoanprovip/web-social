package com.example.demo.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String phone;
    private String email;
    private String password;

    @Builder.Default
    private Boolean locked = false;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lockedUntil;
}
