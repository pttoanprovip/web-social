package com.example.demo.dto.req;

import com.example.demo.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostCreateRequest {
    private String content;
    private String link;
    private Privacy privacy;
}
