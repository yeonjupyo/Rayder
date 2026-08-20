package com.likelion.backend.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.diagnosis.dto.DiagnosisSubmitRequest;
import com.likelion.backend.diagnosis.dto.DiagnosisSubmitResponse;
import com.likelion.backend.diagnosis.dto.GeneratedResultId;
import com.likelion.backend.diagnosis.mapper.DiagnosisMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiagnosisServiceTest {

	@Mock DiagnosisMapper diagnosisMapper;

	private DiagnosisService service() {
		return new DiagnosisService(diagnosisMapper);
	}

	private DiagnosisSubmitRequest request(String... answers) {
		DiagnosisSubmitRequest request = new DiagnosisSubmitRequest();
		request.setUserId(1);
		request.setAnswers(answers.length == 0 ? null : Arrays.asList(answers));
		return request;
	}

	private static String[] repeat(String answer) {
		String[] answers = new String[7];
		Arrays.fill(answers, answer);
		return answers;
	}

	@Test
	void rejectsAWrongNumberOfAnswersWithBadRequest() {
		assertThatThrownBy(() -> service().submit(request("neutral", "neutral")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasFieldOrPropertyWithValue("code", "INVALID_DIAGNOSIS_ANSWERS");
		verify(diagnosisMapper, never()).insertAnswers(any());
	}

	@Test
	void rejectsMissingAnswersWithBadRequest() {
		assertThatThrownBy(() -> service().submit(request()))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
	}

	@Test
	void rejectsAnUnknownAnswerValueWithBadRequest() {
		String[] answers = repeat("neutral");
		answers[3] = "definitely";

		assertThatThrownBy(() -> service().submit(request(answers)))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
			.hasMessageContaining("4번");
	}

	/** 민감 점수(2·3번) 4 이상이면 민감성으로 확정된다. */
	@Test
	void decidesSensitiveSkinFromTheSecondAndThirdAnswers() {
		doAnswer(invocation -> {
			invocation.getArgument(3, GeneratedResultId.class).setResultId(11);
			return null;
		}).when(diagnosisMapper).insertResult(anyInt(), anyString(), anyString(), any());

		String[] answers = repeat("disagree");
		answers[1] = "stronglyAgree";
		answers[2] = "slightlyAgree";

		DiagnosisSubmitResponse response = service().submit(request(answers));

		assertThat(response.getResultId()).isEqualTo(11);
		assertThat(response.getSkinType()).isEqualTo("민감성피부");
		assertThat(response.getKeywords()).containsExactly("붉어짐", "예민함");
		assertThat(response.getDescription()).isNotBlank();
		verify(diagnosisMapper).insertAnswers(any());
	}

	/** 저장되는 답변 7행은 문항 순서와 내용을 그대로 따라간다. */
	@Test
	void storesOneRowPerQuestionInOrder() {
		doAnswer(invocation -> {
			invocation.getArgument(3, GeneratedResultId.class).setResultId(12);
			return null;
		}).when(diagnosisMapper).insertResult(anyInt(), anyString(), anyString(), any());

		service().submit(request(repeat("neutral")));

		verify(diagnosisMapper).insertAnswers(org.mockito.ArgumentMatchers.argThat(rows -> {
			List<?> list = rows;
			return list.size() == 7;
		}));
	}
}
