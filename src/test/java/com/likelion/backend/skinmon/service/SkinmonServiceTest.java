package com.likelion.backend.skinmon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.skinmon.dto.GeneratedSkinmonId;
import com.likelion.backend.skinmon.dto.SkinmonCreateRequest;
import com.likelion.backend.skinmon.dto.SkinmonCreateResponse;
import com.likelion.backend.skinmon.mapper.SkinmonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SkinmonServiceTest {

	@Mock SkinmonMapper skinmonMapper;

	private SkinmonService service() {
		return new SkinmonService(skinmonMapper);
	}

	private SkinmonCreateRequest request() {
		SkinmonCreateRequest request = new SkinmonCreateRequest();
		request.setUserId(1);
		request.setResultId(5);
		request.setSkinmonName("몽이");
		return request;
	}

	@Test
	void createsTheSkinmonWithTheHappyAppearance() {
		when(skinmonMapper.findSkinTypeByResultId(5)).thenReturn("건성");
		when(skinmonMapper.findAppearanceId("건성", "happy")).thenReturn(3);
		doAnswer(invocation -> {
			invocation.getArgument(4, GeneratedSkinmonId.class).setSkinmonId(7);
			return null;
		}).when(skinmonMapper).upsertSkinmon(anyInt(), anyInt(), anyString(), anyInt(), any());

		SkinmonCreateResponse response = service().create(request());

		assertThat(response.getSkinmonId()).isEqualTo(7);
		assertThat(response.getSkinType()).isEqualTo("건성");
		assertThat(response.getExpressionType()).isEqualTo("happy");
		verify(skinmonMapper).upsertSkinmon(org.mockito.ArgumentMatchers.eq(1),
			org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq("몽이"),
			org.mockito.ArgumentMatchers.eq(3), any());
	}

	@Test
	void answersNotFoundWhenTheDiagnosisResultIsMissing() {
		when(skinmonMapper.findSkinTypeByResultId(5)).thenReturn(null);

		assertThatThrownBy(() -> service().create(request()))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
			.hasFieldOrPropertyWithValue("code", "DIAGNOSIS_RESULT_NOT_FOUND");
		verify(skinmonMapper, never()).upsertSkinmon(anyInt(), anyInt(), anyString(), anyInt(), any());
	}

	/** 참조 데이터 누락은 클라이언트가 고칠 수 없으므로 500 이지만 코드로 원인을 알린다. */
	@Test
	void answersServerErrorWhenTheAppearanceRowIsMissing() {
		when(skinmonMapper.findSkinTypeByResultId(5)).thenReturn("건성");
		when(skinmonMapper.findAppearanceId("건성", "happy")).thenReturn(null);

		assertThatThrownBy(() -> service().create(request()))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.INTERNAL_SERVER_ERROR)
			.hasFieldOrPropertyWithValue("code", "SKINMON_APPEARANCE_NOT_FOUND");
	}
}
