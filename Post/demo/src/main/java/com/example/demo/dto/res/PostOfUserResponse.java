package com.example.demo.dto.res;

import com.example.demo.enums.Privacy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostOfUserResponse {
    private String id;
    private String content;
    private String imageURL;
    private String videoURL;
    private String link;
    private Privacy privacy;
    private LocalDate createAt;
    private LocalDate updateAt;
}
