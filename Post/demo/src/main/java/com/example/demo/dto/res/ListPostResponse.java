package com.example.demo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class istPostResponse {
    private boolean success;
    private String message;
    private List<PostOfUserResponse> data;
}
