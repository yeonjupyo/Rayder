package com.likelion.backend.auth.service;

import com.likelion.backend.auth.dto.GeneratedUserId;
import com.likelion.backend.auth.dto.LoginRequest;
import com.likelion.backend.auth.dto.SignUpRequest;
import com.likelion.backend.auth.dto.UserCredentialRow;
import com.likelion.backend.auth.dto.UserDto;
import com.likelion.backend.auth.mapper.UserMapper;
import com.likelion.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입과 로그인. 비밀번호는 BCrypt 해시로만 저장하고 응답에 담지 않는다.
 *
 * <p>토큰은 아직 발급하지 않는다. 클라이언트는 응답의 {@code userId} 로 개발용 세션 토큰을 만들고,
 * 서버는 {@code DevAuthenticationFilter} 로 그 값을 읽는다. 실제 JWT 를 붙일 때 이 서비스가
 * 토큰을 발급하도록 바꾸면 된다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UserDto signUp(SignUpRequest request) {
		if (userMapper.existsByPhone(request.phone())) {
			throw new BusinessException("DUPLICATE_PHONE", "이미 가입된 전화번호예요.", HttpStatus.CONFLICT);
		}

		GeneratedUserId holder = new GeneratedUserId();
		userMapper.insertUser(request.phone(), request.name(),
			passwordEncoder.encode(request.password()), holder);

		return userMapper.findById(holder.getUserId());
	}

	@Transactional(readOnly = true)
	public UserDto logIn(LoginRequest request) {
		UserCredentialRow row = userMapper.findCredentialByIdentifier(request.identifier().trim());
		// 존재하지 않는 계정과 비밀번호 불일치를 같은 응답으로 처리해 계정 존재 여부를 노출하지 않는다.
		if (row == null || row.getPassword() == null
			|| !passwordEncoder.matches(request.password(), row.getPassword())) {
			throw new BusinessException("INVALID_CREDENTIALS",
				"아이디 또는 비밀번호가 올바르지 않아요.", HttpStatus.UNAUTHORIZED);
		}
		return toDto(row);
	}

	private UserDto toDto(UserCredentialRow row) {
		UserDto dto = new UserDto();
		dto.setUserId(row.getUserId());
		dto.setEmail(row.getEmail());
		dto.setPhone(row.getPhone());
		dto.setNickname(row.getNickname());
		dto.setRegion(row.getRegion());
		return dto;
	}
}
