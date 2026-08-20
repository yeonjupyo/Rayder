package com.likelion.backend.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.backend.environment.client.AirKoreaDustClient;
import com.likelion.backend.environment.client.KakaoGeocodingClient;
import com.likelion.backend.environment.client.KmaUvClient;
import com.likelion.backend.environment.client.RegionResolver;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.exception.EnvironmentApiException;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentQueryServiceTest {
	@Mock KmaUvClient kma;
	@Mock AirKoreaDustClient dust;
	@Mock KakaoGeocodingClient kakao;

	@Test
	void classpathRegionResourceResolvesGangnam() {
		var code = new RegionResolver().resolve("서울", "강남구");
		assertThat(code.areaNo()).isEqualTo("1168000000");
	}

	@Test
	void locationBasedUvIsReusableWithoutController() {
		RegionResolver resolver = new RegionResolver();
		var service = new EnvironmentQueryService(kma, dust, kakao, resolver);
		var expected = new EnvironmentInfo(EnvironmentInfo.Type.UV, 4.0, "보통", "서울특별시 강남구", LocalDateTime.now());
		when(kakao.resolveRegion(37.4979, 127.0276)).thenReturn(new KakaoGeocodingClient.GeoRegion("서울특별시", "강남구", "역삼동"));
		when(kma.getUvIndex("1168000000", "서울특별시 강남구")).thenReturn(expected);

		assertThat(service.getUvByLocation(37.4979, 127.0276)).isSameAs(expected);
		verify(kma).getUvIndex("1168000000", "서울특별시 강남구");
	}

	@Test
	void upstreamFailureIsNotHidden() {
		var service = new EnvironmentQueryService(kma, dust, kakao, new RegionResolver());
		when(kakao.resolveRegion(37.4979, 127.0276)).thenThrow(new EnvironmentApiException("Kakao failed"));
		assertThatThrownBy(() -> service.getDustByLocation(37.4979, 127.0276))
			.isInstanceOf(EnvironmentApiException.class).hasMessage("Kakao failed");
	}

	@Test
	void unknownRegionIsClientError() {
		assertThatThrownBy(() -> new RegionResolver().resolve("없는도", "없는구"))
			.isInstanceOf(EnvironmentApiException.class)
			.extracting(e -> ((EnvironmentApiException) e).getStatus().value()).isEqualTo(400);
	}
}
