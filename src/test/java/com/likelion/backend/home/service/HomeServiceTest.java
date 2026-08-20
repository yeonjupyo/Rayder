package com.likelion.backend.home.service;

import com.likelion.backend.environment.client.KmaUvClient;
import com.likelion.backend.home.dto.HomeResponse;
import com.likelion.backend.home.dto.UvForecastPoint;
import com.likelion.backend.home.mapper.HomeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock HomeMapper homeMapper;
    @Mock KmaUvClient kmaUvClient;
    @InjectMocks HomeService homeService;

    /**
     * KmaUvClient 는 3시간 간격으로 h0~h75(3일치)를 준다. 홈 응답은 발표시각 기준 오프셋으로
     * 하루치(0~24h)만 담아야 하고, 노출량은 이미 지난 구간만 누적해야 한다.
     */
    @Test
    void mapsForecastToOffsetsWithinOneDayAndAccumulatesOnlyElapsedPoints() {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(6);
        List<com.likelion.backend.environment.dto.UvForecastPoint> points = new ArrayList<>();
        for (int offset = 0; offset <= 75; offset += 3) {
            points.add(new com.likelion.backend.environment.dto.UvForecastPoint(
                    baseTime.plusHours(offset), offset == 30 ? 11.0 : 2.0, "보통"));
        }
        when(kmaUvClient.getUvForecast("1168000000")).thenReturn(points);
        when(homeMapper.findSkinTypeByUserId(1)).thenReturn("건성");

        HomeResponse response = homeService.getHome(1, "1168000000");

        // 0, 3, 6, ... 24 → 9개. h30 의 11.0 은 창을 넘어가므로 포함되지 않는다.
        assertThat(response.getHourlyForecast()).hasSize(9);
        assertThat(response.getHourlyForecast()).extracting(UvForecastPoint::getHourOffset)
                .containsExactly(0, 3, 6, 9, 12, 15, 18, 21, 24);
        assertThat(response.getMaxUvToday()).isEqualByComparingTo(BigDecimal.valueOf(2.0));

        // baseTime 이 6시간 전이므로 지난 구간은 h0, h3, h6 세 개. 2.0 × 3 = 6.0
        // exposureRate = 6/63 × 100 = 9.5200 (소수 4자리 반올림 후 ×100)
        assertThat(response.getExposureRate()).isEqualByComparingTo(new BigDecimal("9.5200"));
        assertThat(response.getExpressionType()).isEqualTo("happy");
    }
}
