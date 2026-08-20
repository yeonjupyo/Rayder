package com.likelion.backend.common.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 임시 인증 처리.
 * 실제 로그인/JWT를 안 만들기로 한 팀 결정에 맞춰,
 * 모든 요청에 테스트 계정(user_id=1)을 인증된 사용자로 고정 세팅한다.
 * 루틴/알림/AI 등 @RequestAttribute("authenticatedUserId")를 요구하는
 * 컨트롤러들이 이 필터 없이는 호출 자체가 안 되는 문제를 해결하기 위함.
 *
 * TODO: 실제 로그인 붙이게 되면 이 필터는 삭제하고 JWT 필터로 교체.
 */
@Component
public class TempAuthFilter implements Filter {

    private static final long TEST_USER_ID = 1L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setAttribute("authenticatedUserId", TEST_USER_ID);
        chain.doFilter(request, response);
    }
}