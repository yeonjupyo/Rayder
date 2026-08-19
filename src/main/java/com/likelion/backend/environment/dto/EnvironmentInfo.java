package com.likelion.backend.environment.dto;

import java.time.LocalDateTime;

public record EnvironmentInfo(Type type, Double value, String level, String region, LocalDateTime observedAt) {
	public enum Type { UV, DUST_PM10, DUST_PM25 }
}
