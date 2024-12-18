package com.sbs.spring1012.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization"; // 토큰을 요청 헤더의 Authorization 키에 담어서 전달
    public static final String BEARER_PREFIX = "Bearer "; // 토큰 앞에 붙이는 문자열
    private final TokenProvider tokenProvider; // 토큰 생성, 토큰 검증을 수행하는 TokenProvider

    private String resolveToken(HttpServletRequest request) { // 토큰을 요청 헤더에서 꺼내오는 메서드
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER); // 헤더에서 토큰 꺼내오기
        if(bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) { // 토큰이 존재하고, 토큰 앞에 붙는 문자열이 존재하면
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = resolveToken(request);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Authentication authentication = tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}