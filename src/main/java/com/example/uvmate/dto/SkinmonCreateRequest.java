package com.example.uvmate.dto;

import lombok.Data;

@Data
public class SkinmonCreateRequest {
    private int userId;
    private int resultId;
    private String skinmonName;
}