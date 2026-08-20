package com.likelion.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 로그인 요청. {@code identifier} 는 휴대폰 번호(숫자만) 또는 이메일이다. */
public record LoginRequest(
	@NotBlank String identifier,
	@NotBlank String password
) {
}
