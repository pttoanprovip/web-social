package com.example.demo.dto.req;

import com.example.demo.enums.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLikeRequest {
    private Type type;
}
