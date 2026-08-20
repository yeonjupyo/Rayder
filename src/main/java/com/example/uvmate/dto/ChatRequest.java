package com.example.uvmate.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private int userId;
    private String message;
}