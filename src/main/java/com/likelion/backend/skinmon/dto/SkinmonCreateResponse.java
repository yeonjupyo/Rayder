package com.likelion.backend.skinmon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkinmonCreateResponse {
    private int skinmonId;
    private String skinmonName;
    private String skinType;
    private String expressionType;
}