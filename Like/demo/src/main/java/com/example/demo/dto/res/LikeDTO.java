package com.example.demo.dto.res;

import com.example.demo.enums.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeDTO {
    private String id;
    private String userId;
    private String postId;
    private String commentId;
    private Type type;
    private String avatar;
    private String fName;
    private String lName;
    private LocalDate createdAt;
}
