package com.example.demo.dto.req;

import com.example.demo.enums.Privacy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {
    private String content;
    private String link;
    private Privacy privacy;
}
