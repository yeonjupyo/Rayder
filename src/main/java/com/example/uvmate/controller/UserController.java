package com.example.uvmate.controller;

import com.example.uvmate.dto.UserDto;
import com.example.uvmate.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @PostMapping("/login")
    public ResponseEntity<UserDto> login() {
        // DB의 1번 테스트 계정 정보를 가져와서 반환
        return ResponseEntity.ok(userMapper.getTestUser());
    }
}