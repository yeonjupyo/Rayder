package com.likelion.backend.home.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UvForecastPoint {
    private int hourOffset; // 0, 3, 6, 9 ... (발표시각 기준 몇 시간 후)
    /** 이 구간의 절대 시각. 클라이언트가 x축 시각 라벨을 그릴 때 쓴다. */
    private LocalDateTime forecastAt;
    private double value;
}
