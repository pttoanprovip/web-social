package com.example.demo.event;

import com.example.demo.enums.Privacy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdatedEvent {
    private String id;
    private String content;
    private String link;
    private String imageURL;
    private String videoURL;
    private Privacy privacy;
    private LocalDate updateAt;
}
