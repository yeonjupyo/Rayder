package com.likelion.backend.skinmon.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SkinmonExpressionUpdateRequest {

    /** SKINMON_APPEARANCE 에 등록된 표정은 happy / sad 두 가지다. */
    @Pattern(regexp = "happy|sad", message = "표정은 happy 또는 sad 여야 한다")
    private String expressionType;
}
