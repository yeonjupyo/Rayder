package com.example.uvmate.environment.client;

import com.example.uvmate.environment.exception.EnvironmentApiException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 기상청이 배포하는 지역코드 엑셀(dfs-zone-tree)에서 구/군 단위 256개 행만
 * 뽑아 resources/kma-area-codes.csv 로 미리 만들어둔 걸 읽어서,
 * "서울특별시", "강남구" 같은 이름으로 areaNo를 찾아주는 컴포넌트.
 *
 * CSV 컬럼: sido, gugun, areaNo, gridX, gridY
 * (gridX/gridY는 GridConverter가 계산한 값과 대조해서 검증하는 용도로 같이 둠)
 */
@Component
public class RegionResolver {

    private static final String CSV_PATH = "kma-area-codes.csv";

    public record RegionCode(String sido, String gugun, String areaNo, int gridX, int gridY) {
    }

    private final List<RegionCode> codes = new ArrayList<>();

    public RegionResolver() {
        loadCsv();
    }

    private void loadCsv() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // 헤더 skip
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 5) continue;
                codes.add(new RegionCode(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        Integer.parseInt(parts[3].trim()),
                        Integer.parseInt(parts[4].trim())
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("kma-area-codes.csv 로드 실패 - resources 폴더에 파일이 있는지 확인", e);
        }
    }

    /**
     * @param sidoKeyword  "서울", "서울특별시" 등 부분 일치 가능
     * @param gugunKeyword "강남구" 등
     */
    public RegionCode resolve(String sidoKeyword, String gugunKeyword) {
        return codes.stream()
                .filter(c -> c.sido().contains(sidoKeyword) && c.gugun().equals(gugunKeyword))
                .findFirst()
                .orElseThrow(() -> new EnvironmentApiException(
                        "'" + sidoKeyword + " " + gugunKeyword + "'에 해당하는 지역코드를 찾을 수 없음 "
                                + "(kma-area-codes.csv에 없는 지역이거나 이름 표기가 다를 수 있음)"));
    }
}
