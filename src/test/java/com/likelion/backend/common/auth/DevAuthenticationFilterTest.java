package com.likelion.backend.common.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DevAuthenticationFilterTest {

	private final DevAuthenticationFilter filter =
		new DevAuthenticationFilter(new DevAuthenticationFilter.DevAuthProperties(true, 1L));

	@Test
	void readsUserIdFromDevBearerToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/routines");
		request.addHeader("Authorization", "Bearer dev.7.1755000000000");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(request.getAttribute(DevAuthenticationFilter.AUTHENTICATED_USER_ID)).isEqualTo(7L);
	}

	@Test
	void readsUserIdFromDevHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/routines");
		request.addHeader("X-Dev-User-Id", "42");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(request.getAttribute(DevAuthenticationFilter.AUTHENTICATED_USER_ID)).isEqualTo(42L);
	}

	@Test
	void fallsBackToTheDefaultUserWhenNoUsableHeaderIsPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/routines");
		request.addHeader("Authorization", "Bearer some.opaque.jwt");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(request.getAttribute(DevAuthenticationFilter.AUTHENTICATED_USER_ID)).isEqualTo(1L);
	}
}
