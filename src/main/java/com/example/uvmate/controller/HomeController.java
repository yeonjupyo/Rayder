package com.example.uvmate.controller;

import com.example.uvmate.dto.HomeResponse;
import com.example.uvmate.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ResponseEntity<HomeResponse> getHome(@RequestParam int userId, @RequestParam String areaNo) {
        return ResponseEntity.ok(homeService.getHome(userId, areaNo));
    }
}