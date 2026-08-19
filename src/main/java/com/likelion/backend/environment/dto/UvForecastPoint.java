package com.likelion.backend.environment.dto;

import java.time.LocalDateTime;

public record UvForecastPoint(LocalDateTime forecastAt, double value, String level) {
	public boolean isDangerous() { return value >= 6; }
}
