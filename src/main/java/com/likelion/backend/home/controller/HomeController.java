package com.likelion.backend.home.controller;

import com.likelion.backend.home.dto.HomeResponse;
import com.likelion.backend.home.service.HomeService;
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
