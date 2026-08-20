package com.uvmate.environment.dto;

import java.time.LocalDateTime;

/**
 * UV / 미세먼지 API 응답을 프론트/DB 저장용으로 통일한 공통 포맷.
 * 소스(기상청 or 에어코리아)가 달라도 이 형태로 맞춰서 내려준다.
 */
public class EnvironmentInfo {

    public enum Type {
        UV,
        DUST_PM10,
        DUST_PM25
    }

    private Type type;
    private Double value;          // 지수/농도 값
    private String level;          // 낮음/보통/높음/매우높음 등 등급
    private String region;         // 서울시 강남구 등 사용자에게 보여줄 지역명
    private LocalDateTime observedAt; // 관측/발표 시각

    public EnvironmentInfo() {
    }

    public EnvironmentInfo(Type type, Double value, String level, String region, LocalDateTime observedAt) {
        this.type = type;
        this.value = value;
        this.level = level;
        this.region = region;
        this.observedAt = observedAt;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }
}
