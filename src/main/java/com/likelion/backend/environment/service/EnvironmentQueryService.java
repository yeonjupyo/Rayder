package com.likelion.backend.environment.service;

import com.likelion.backend.environment.client.AirKoreaDustClient;
import com.likelion.backend.environment.client.KakaoGeocodingClient;
import com.likelion.backend.environment.client.KmaUvClient;
import com.likelion.backend.environment.client.RegionResolver;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.util.SidoNameConverter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentQueryService {
	private final KmaUvClient kmaUvClient;
	private final AirKoreaDustClient dustClient;
	private final KakaoGeocodingClient geocodingClient;
	private final RegionResolver regionResolver;

	public EnvironmentQueryService(KmaUvClient kmaUvClient, AirKoreaDustClient dustClient,
		KakaoGeocodingClient geocodingClient, RegionResolver regionResolver) {
		this.kmaUvClient = kmaUvClient;
		this.dustClient = dustClient;
		this.geocodingClient = geocodingClient;
		this.regionResolver = regionResolver;
	}

	public EnvironmentInfo getUv(String sido, String gugun) {
		var code = regionResolver.resolve(sido, gugun);
		return kmaUvClient.getUvIndex(code.areaNo(), label(code));
	}

	public List<EnvironmentInfo> getDust(String sido, String gugun) {
		var code = regionResolver.resolve(sido, gugun);
		String label = label(code);
		String sidoShort = SidoNameConverter.toShort(code.sido());
		return List.of(dustClient.getPm10(sidoShort, code.gugun(), label),
			dustClient.getPm25(sidoShort, code.gugun(), label));
	}

	public KakaoGeocodingClient.GeoRegion getLocation(double lat, double lon) {
		return geocodingClient.resolveRegion(lat, lon);
	}

	public EnvironmentInfo getUvByLocation(double lat, double lon) {
		var region = getLocation(lat, lon);
		return getUv(region.sido(), region.gugun());
	}

	public List<EnvironmentInfo> getDustByLocation(double lat, double lon) {
		var region = getLocation(lat, lon);
		return getDust(region.sido(), region.gugun());
	}

	private String label(RegionResolver.RegionCode code) { return code.sido() + " " + code.gugun(); }
}
