package com.example.uvmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UvForecastPoint {
    private int hourOffset; // 0, 3, 6, 9 ... (발표시각 기준 몇 시간 후)
    private double value;
}
