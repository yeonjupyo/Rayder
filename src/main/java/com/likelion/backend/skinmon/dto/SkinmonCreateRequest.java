package com.likelion.backend.skinmon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkinmonCreateRequest {

    @Min(value = 1, message = "userId 가 필요하다")
    private int userId;

    @Min(value = 1, message = "resultId 가 필요하다")
    private int resultId;

    /** SKINMON.skinmon_name 이 varchar(20) 이다. */
    @NotBlank(message = "스킨몽 이름이 필요하다")
    @Size(max = 20, message = "스킨몽 이름은 20자 이내여야 한다")
    private String skinmonName;
}
