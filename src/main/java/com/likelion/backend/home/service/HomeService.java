package com.likelion.backend.home.service;

import com.likelion.backend.home.dto.DailyUvStatusRow;
import com.likelion.backend.home.dto.HomeResponse;
import com.likelion.backend.home.dto.UvForecastPoint;
import com.likelion.backend.environment.client.KmaUvClient;
import com.likelion.backend.home.mapper.HomeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final BigDecimal SAD_THRESHOLD = new BigDecimal("80");

    // 기상청 UV 등급 기준(resolveUvLevel의 "높음" 상한=7)을 하루 종일 유지했다고 가정한 값을 100%로 설정.
    // 3시간 간격 예보 9구간(h0~h24) × 7 = 63
    private static final BigDecimal DAILY_EXPOSURE_THRESHOLD = new BigDecimal("63");

    /** 홈 화면이 노출하는 예보 구간. KmaUvClient 는 h75 까지 주지만 홈은 하루치만 쓴다. */
    private static final int FORECAST_WINDOW_HOURS = 24;

    private final HomeMapper homeMapper;
    private final KmaUvClient kmaUvClient;

    /** 오늘자 노출량 행을 upsert 한 뒤 같은 행을 다시 읽으므로 한 트랜잭션으로 묶는다. */
    @Transactional
    public HomeResponse getHome(int userId, String areaNo) {
        String skinType = homeMapper.findSkinTypeByUserId(userId);
        if (skinType == null) {
            throw new IllegalStateException("스킨몽이 아직 생성되지 않았습니다: userId=" + userId);
        }

        // KmaUvClient 는 발표시각 기준 절대시각(forecastAt)으로 h0~h75 를 준다.
        // 홈 응답 계약은 "발표시각 기준 몇 시간 후(hourOffset)" 의 하루치이므로 오프셋으로 되돌리고 24h 에서 끊는다.
        List<com.likelion.backend.environment.dto.UvForecastPoint> points = kmaUvClient.getUvForecast(areaNo);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseTime = points.isEmpty() ? now : points.get(0).forecastAt();

        List<UvForecastPoint> hourlyForecast = new ArrayList<>();
        BigDecimal elapsedSum = BigDecimal.ZERO;
        BigDecimal maxUvToday = BigDecimal.ZERO;
        BigDecimal currentUv = BigDecimal.ZERO;

        for (com.likelion.backend.environment.dto.UvForecastPoint point : points) {
            int hourOffset = (int) Duration.between(baseTime, point.forecastAt()).toHours();
            if (hourOffset > FORECAST_WINDOW_HOURS) {
                break;
            }
            hourlyForecast.add(new UvForecastPoint(hourOffset, point.forecastAt(), point.value()));

            BigDecimal value = BigDecimal.valueOf(point.value());
            if (value.compareTo(maxUvToday) > 0) {
                maxUvToday = value;
            }
            // 이미 지난 예보 구간만 노출량에 누적한다. 오프셋과 벽시계 시를 비교하면 단위가 어긋나므로
            // 절대시각끼리 비교한다.
            if (!point.forecastAt().isAfter(now)) {
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
