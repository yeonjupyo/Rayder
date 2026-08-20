package com.example.uvmate.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DailyUvStatusRow {
    private BigDecimal uvIndex;
    private Integer dustIndex;
    private BigDecimal exposureRate;
    private BigDecimal maxUvToday;
}