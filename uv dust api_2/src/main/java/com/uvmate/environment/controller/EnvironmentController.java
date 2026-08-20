package com.uvmate.environment.controller;

import com.uvmate.environment.client.AirKoreaDustClient;
import com.uvmate.environment.client.KakaoGeocodingClient;
import com.uvmate.environment.client.KmaUvClient;
import com.uvmate.environment.client.RegionResolver;
import com.uvmate.environment.client.SidoNameConverter;
import com.uvmate.environment.dto.EnvironmentInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 두 가지 방식 제공:
 * 1) 이름 직접 지정: sido("서울"), gugun("강남구") - 지역 선택 UI 등에서 사용
 * 2) 현재 위치 기반: lat, lon(GPS 좌표) - 카카오 로컬 API로 자동 변환 후 1)과 동일하게 처리
 *
 * areaNo(기상청)는 RegionResolver가 kma-area-codes.csv에서 찾아서 자동 변환.
 * sidoName(에어코리아)은 SidoNameConverter로 축약형("서울특별시"->"서울")으로 변환해서 사용.
 */
@RestController
public class EnvironmentController {

    private final KmaUvClient kmaUvClient;
    private final AirKoreaDustClient airKoreaDustClient;
    private final RegionResolver regionResolver;
    private final KakaoGeocodingClient kakaoGeocodingClient;

    public EnvironmentController(KmaUvClient kmaUvClient,
                                  AirKoreaDustClient airKoreaDustClient,
                                  RegionResolver regionResolver,
                                  KakaoGeocodingClient kakaoGeocodingClient) {
        this.kmaUvClient = kmaUvClient;
        this.airKoreaDustClient = airKoreaDustClient;
        this.regionResolver = regionResolver;
        this.kakaoGeocodingClient = kakaoGeocodingClient;
    }

    // ---------- 이름으로 직접 지정 ----------

    @GetMapping("/api/environment/uv")
    public EnvironmentInfo getUv(@RequestParam String sido, @RequestParam String gugun) {
        RegionResolver.RegionCode code = regionResolver.resolve(sido, gugun);
        String regionLabel = code.sido() + " " + code.gugun();
        return kmaUvClient.getUvIndex(code.areaNo(), regionLabel);
    }

    @GetMapping("/api/environment/dust")
    public List<EnvironmentInfo> getDust(@RequestParam String sido, @RequestParam String gugun) {
        // areaNo 조회를 먼저 해서 지역명이 실제 존재하는지 검증 + 표시용 정식 지역명 확보
        RegionResolver.RegionCode code = regionResolver.resolve(sido, gugun);
        String regionLabel = code.sido() + " " + code.gugun();
        String sidoShort = SidoNameConverter.toShort(code.sido());

        return List.of(
                airKoreaDustClient.getPm10(sidoShort, gugun, regionLabel),
                airKoreaDustClient.getPm25(sidoShort, gugun, regionLabel)
        );
    }

    // ---------- 현재 위치(GPS 좌표) 기반 ----------

    @GetMapping("/api/environment/uv/by-location")
    public EnvironmentInfo getUvByLocation(@RequestParam double lat, @RequestParam double lon) {
        KakaoGeocodingClient.GeoRegion region = kakaoGeocodingClient.resolveRegion(lat, lon);
        return getUv(region.sido(), region.gugun());
    }

    @GetMapping("/api/environment/dust/by-location")
    public List<EnvironmentInfo> getDustByLocation(@RequestParam double lat, @RequestParam double lon) {
        KakaoGeocodingClient.GeoRegion region = kakaoGeocodingClient.resolveRegion(lat, lon);
        return getDust(region.sido(), region.gugun());
    }

    // ---------- 순수 행정구역 조회 (UV/미세먼지 없이 위치 -> 동/구/시 이름만) ----------

    @GetMapping("/api/location")
    public KakaoGeocodingClient.GeoRegion getLocation(@RequestParam double lat, @RequestParam double lon) {
        return kakaoGeocodingClient.resolveRegion(lat, lon);
    }
}
