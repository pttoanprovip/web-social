package com.example.demo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.example.demo.enums.Privacy;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateEvent {
    private String id;
    private String authorId;
    private String content;
    private String link;
    private String imageURL;
    private String videoURL;
    private Privacy privacy;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatar;
    private LocalDate createAt;
}
