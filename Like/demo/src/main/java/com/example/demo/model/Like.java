package com.example.demo.model;

import com.example.demo.enums.Type;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "likes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String postId;
    private String commentId;

    @Enumerated(EnumType.STRING)
    private Type type;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createAt;
}
