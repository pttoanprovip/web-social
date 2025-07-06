package com.example.demo.dto.res;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private String id;
    private String postId;
    private String userId;
    private String content;
    private String imageURL;
    private String videoURL;
    private LocalDate createAt;
    private String avatar;
    private String fname;
    private String lname;
    private String parentCommentId;
}
