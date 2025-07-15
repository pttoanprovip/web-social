package com.example.demo.dto.req;

import com.example.demo.enums.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLikeRequest {
    private String postId;
    private String commentId;
    private Type type;
}
