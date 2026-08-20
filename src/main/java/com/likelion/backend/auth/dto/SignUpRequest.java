package com.likelion.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청. 로그인 식별자는 휴대폰 번호이며 숫자만 받는다(프론트에서 하이픈을 제거해 보낸다).
 */
public record SignUpRequest(
	@NotBlank @Size(min = 2, max = 50) String name,
	@NotBlank @Pattern(regexp = "0\\d{9,10}", message = "휴대폰 번호는 숫자만, 10~11자리로 보내야 한다") String phone,
	@NotBlank @Size(min = 8, max = 72) String password
) {
}
