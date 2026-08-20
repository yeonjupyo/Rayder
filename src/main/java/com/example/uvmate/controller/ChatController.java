package com.example.uvmate.controller;

import com.example.uvmate.dto.ChatRequest;
import com.example.uvmate.dto.ChatResponse;
import com.example.uvmate.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }
}