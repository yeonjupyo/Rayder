package com.likelion.backend.environment.client;

import com.likelion.backend.environment.exception.EnvironmentApiException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class RegionResolver {
	public record RegionCode(String sido, String gugun, String areaNo, int gridX, int gridY) { }
	private final List<RegionCode> codes = new ArrayList<>();

	public RegionResolver() { loadCsv(); }

	private void loadCsv() {
		try (var reader = new BufferedReader(new InputStreamReader(
			new ClassPathResource("kma-area-codes.csv").getInputStream(), StandardCharsets.UTF_8))) {
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) continue;
				String[] p = line.split(",", -1);
				if (p.length >= 5) codes.add(new RegionCode(p[0].trim(), p[1].trim(), p[2].trim(),
					Integer.parseInt(p[3].trim()), Integer.parseInt(p[4].trim())));
			}
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("Failed to load classpath resource kma-area-codes.csv", e);
		}
	}

	public RegionCode resolve(String sido, String gugun) {
		if (sido == null || sido.isBlank() || gugun == null || gugun.isBlank())
			throw EnvironmentApiException.invalidInput("sido and gugun must not be blank");
		String normalizedSido = sido.trim();
		String normalizedGugun = gugun.trim();
		return codes.stream().filter(c -> c.sido().contains(normalizedSido) && c.gugun().equals(normalizedGugun))
			.findFirst().orElseThrow(() -> EnvironmentApiException.regionNotFound(
				"Region code not found: " + normalizedSido + " " + normalizedGugun));
	}
}
