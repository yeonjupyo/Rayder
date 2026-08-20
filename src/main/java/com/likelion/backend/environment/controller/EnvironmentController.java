package com.likelion.backend.environment.controller;

import com.likelion.backend.environment.client.KakaoGeocodingClient;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentController {
	private final EnvironmentQueryService service;
	public EnvironmentController(EnvironmentQueryService service) { this.service = service; }

	@GetMapping("/api/environment/uv")
	public EnvironmentInfo getUv(@RequestParam String sido, @RequestParam String gugun) { return service.getUv(sido, gugun); }

	@GetMapping("/api/environment/dust")
	public List<EnvironmentInfo> getDust(@RequestParam String sido, @RequestParam String gugun) { return service.getDust(sido, gugun); }

	@GetMapping("/api/environment/uv/by-location")
	public EnvironmentInfo getUvByLocation(@RequestParam double lat, @RequestParam double lon) { return service.getUvByLocation(lat, lon); }

	@GetMapping("/api/environment/dust/by-location")
	public List<EnvironmentInfo> getDustByLocation(@RequestParam double lat, @RequestParam double lon) { return service.getDustByLocation(lat, lon); }

	@GetMapping("/api/location")
	public KakaoGeocodingClient.GeoRegion getLocation(@RequestParam double lat, @RequestParam double lon) { return service.getLocation(lat, lon); }
}
