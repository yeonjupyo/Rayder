package com.example.uvmate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class HomeResponse {
    private BigDecimal uvIndex;
    private Integer dustIndex;
    private BigDecimal exposureRate;
    private BigDecimal maxUvToday;
    private String skinType;
    private String expressionType;
    private List<UvForecastPoint> hourlyForecast;
}