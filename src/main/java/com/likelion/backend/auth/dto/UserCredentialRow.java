package com.likelion.backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

/** 로그인 검증용 조회 결과. 비밀번호 해시를 담고 있으므로 응답에 그대로 내보내지 않는다. */
@Getter
@Setter
public class UserCredentialRow {
    private Long userId;
    private String email;
    private String phone;
    private String nickname;
    private String region;
    private String password;
}
