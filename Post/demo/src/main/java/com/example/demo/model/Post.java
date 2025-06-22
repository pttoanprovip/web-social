package com.example.demo.model;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.example.demo.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String authorId;
    
    private String content;
    private String imageURL;
    private String videoURL;
    private String link;
    private Privacy privacy;
    private LocalDate createAt;
    private LocalDate updateAt;
}
