package com.example.demo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistedEvent {
    private String id;
    private String fName;
    private String lName;
    private String avatar;
}
