package com.likelion.backend.diagnosis.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DiagnosisSubmitRequest {

    @Min(value = 1, message = "userId 가 필요하다")
    private int userId;

    /** QUESTIONS 순서와 동일한 7개. 허용 값은 DiagnosisConstants.scoreOf 참고. */
    @NotNull(message = "answers 가 필요하다")
    @Size(min = 7, max = 7, message = "답변은 7개여야 한다")
    private List<@NotEmpty String> answers;
}
