package com.likelion.backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private Long userId;
    private String email;
    private String nickname;
    private String region;
}
