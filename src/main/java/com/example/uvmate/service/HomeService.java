package com.example.uvmate.service;

import com.example.uvmate.dto.DailyUvStatusRow;
import com.example.uvmate.dto.HomeResponse;
import com.example.uvmate.dto.UvForecastPoint;
import com.example.uvmate.environment.client.KmaUvClient;
import com.example.uvmate.mapper.HomeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final BigDecimal SAD_THRESHOLD = new BigDecimal("80");

    // 기상청 UV 등급 기준(resolveUvLevel의 "높음" 상한=7)을 하루 종일 유지했다고 가정한 값을 100%로 설정.
// 3시간 간격 예보 9구간(h0~h24) × 7 = 63
    private static final BigDecimal DAILY_EXPOSURE_THRESHOLD = new BigDecimal("63");

    private final HomeMapper homeMapper;
    private final KmaUvClient kmaUvClient;

    public HomeResponse getHome(int userId, String areaNo) {
        String skinType = homeMapper.findSkinTypeByUserId(userId);
        if (skinType == null) {
            throw new IllegalStateException("스킨몽이 아직 생성되지 않았습니다: userId=" + userId);
        }

        List<UvForecastPoint> hourlyForecast = kmaUvClient.getUvForecastToday(areaNo);

        // 현재 시각까지 지난 예보 구간만 합산 (0~24시간 후 값 중 현재 시각 이하인 것)
        int currentHour = LocalTime.now().getHour();
        BigDecimal elapsedSum = BigDecimal.ZERO;
        BigDecimal maxUvToday = BigDecimal.ZERO;
        BigDecimal currentUv = BigDecimal.ZERO;

        for (UvForecastPoint point : hourlyForecast) {
            BigDecimal value = BigDecimal.valueOf(point.getValue());
            if (value.compareTo(maxUvToday) > 0) {
                maxUvToday = value;
            }
            if (point.getHourOffset() <= currentHour) {
                elapsedSum = elapsedSum.add(value);
                currentUv = value; // 마지막으로 지난 구간 값 = 현재 UV로 사용
            }
        }

        BigDecimal exposureRate = elapsedSum
                .divide(DAILY_EXPOSURE_THRESHOLD, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100));

        homeMapper.upsertTodayUvStatus(userId, currentUv, exposureRate, maxUvToday);

        DailyUvStatusRow status = homeMapper.findTodayUvStatus(userId);

        String expressionType = exposureRate.compareTo(SAD_THRESHOLD) >= 0 ? "sad" : "happy";

        return new HomeResponse(
                currentUv,
                status != null ? status.getDustIndex() : null,
                exposureRate,
                maxUvToday,
                skinType,
                expressionType,
                hourlyForecast
        );
    }
}