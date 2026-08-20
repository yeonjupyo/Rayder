package com.likelion.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.backend.auth.dto.GeneratedUserId;
import com.likelion.backend.auth.dto.LoginRequest;
import com.likelion.backend.auth.dto.SignUpRequest;
import com.likelion.backend.auth.dto.UserCredentialRow;
import com.likelion.backend.auth.dto.UserDto;
import com.likelion.backend.auth.mapper.UserMapper;
import com.likelion.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock UserMapper userMapper;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private AuthService authService;

	private AuthService service() {
		if (authService == null) {
			authService = new AuthService(userMapper, encoder);
		}
		return authService;
	}

	@Test
	void storesOnlyAHashOnSignUp() {
		when(userMapper.existsByPhone("01012345678")).thenReturn(false);
		doAnswer(invocation -> {
			invocation.getArgument(3, GeneratedUserId.class).setUserId(9L);
			return null;
		}).when(userMapper).insertUser(anyString(), anyString(), anyString(), any());
		when(userMapper.findById(9L)).thenReturn(new UserDto());

		service().signUp(new SignUpRequest("테스터", "01012345678", "P@ssw0rd"));

		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(userMapper).insertUser(eq("01012345678"), eq("테스터"), stored.capture(), any());
		assertThat(stored.getValue()).isNotEqualTo("P@ssw0rd").startsWith("$2");
		assertThat(encoder.matches("P@ssw0rd", stored.getValue())).isTrue();
	}

	@Test
	void rejectsADuplicatePhone() {
		when(userMapper.existsByPhone("01012345678")).thenReturn(true);

		assertThatThrownBy(() -> service().signUp(new SignUpRequest("테스터", "01012345678", "P@ssw0rd")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
		verify(userMapper, never()).insertUser(anyString(), anyString(), anyString(), any());
	}

	@Test
	void signsInWithAMatchingPassword() {
		when(userMapper.findCredentialByIdentifier("01012345678"))
			.thenReturn(credential(encoder.encode("P@ssw0rd")));

		UserDto user = service().logIn(new LoginRequest("01012345678", "P@ssw0rd"));

		assertThat(user.getUserId()).isEqualTo(7L);
		assertThat(user.getPhone()).isEqualTo("01012345678");
	}

	@Test
	void rejectsAWrongPassword() {
		when(userMapper.findCredentialByIdentifier("01012345678"))
			.thenReturn(credential(encoder.encode("P@ssw0rd")));

		assertThatThrownBy(() -> service().logIn(new LoginRequest("01012345678", "wrong-password")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rejectsAnUnknownIdentifierWithTheSameError() {
		when(userMapper.findCredentialByIdentifier("01099999999")).thenReturn(null);

		assertThatThrownBy(() -> service().logIn(new LoginRequest("01099999999", "P@ssw0rd")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
	}

	/** 비밀번호가 없는 시드 계정으로는 로그인할 수 없어야 한다. */
	@Test
	void rejectsAnAccountWithoutAPassword() {
		when(userMapper.findCredentialByIdentifier("01000000000")).thenReturn(credential(null));

		assertThatThrownBy(() -> service().logIn(new LoginRequest("01000000000", "P@ssw0rd")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
	}

	private UserCredentialRow credential(String passwordHash) {
		UserCredentialRow row = new UserCredentialRow();
		row.setUserId(7L);
		row.setPhone("01012345678");
		row.setNickname("테스터");
		row.setPassword(passwordHash);
		return row;
	}
}
