package com.likelion.backend.chat.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private int userId;
    private String message;
}
