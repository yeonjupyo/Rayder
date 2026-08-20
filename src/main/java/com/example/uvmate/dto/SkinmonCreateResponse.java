package com.example.uvmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkinmonCreateResponse {
    private int skinmonId;
    private String skinmonName;
    private String skinType;
    private String expressionType;
}