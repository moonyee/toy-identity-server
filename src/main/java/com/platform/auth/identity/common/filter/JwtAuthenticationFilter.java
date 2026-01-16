package com.platform.auth.identity.common.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.identity.common.response.ApiResponseEntity;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey signingKey;
    private final StringRedisTemplate redisTemplate; // MVC용 템플릿
    private final ObjectMapper objectMapper;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> LOGIN_PATHS = List.of("/api/auth/login", "/auth/login");

    public JwtAuthenticationFilter(
        @Value("${jwt.secret:my_super_secret_key_that_is_long_enough_and_random}") String secretKey,
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("### Request received for path: {}", path);

        // 1. 로그인 경로는 필터를 건너뜁니다.
        if (LOGIN_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Authorization 헤더 확인
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("### No Authorization header or invalid format");
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, "No valid authorization header");
            return;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 3. JWT 파싱 및 검증
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

            String username = claims.getSubject();
            log.info("### JWT parsed for user: {}", username);

            // 4. Redis 세션 체크 (핵심 추가 로직)
            String redisKey = "AUTH:" + username + ":" + jwt;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
                log.warn("### Session not found in Redis for user: {}", username);
                setErrorResponse(HttpStatus.UNAUTHORIZED, response, "Session expired or logged out");
                return;
            }

            // 5. SecurityContext에 인증 정보 저장 (ThreadLocal 방식)
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 6. 다음 필터로 진행
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.error("### JWT token is expired: {}", e.getMessage());
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, "JWT token is expired");
        } catch (Exception e) {
            log.error("### Invalid JWT token or filter error: {}", e.getMessage());
            setErrorResponse(HttpStatus.UNAUTHORIZED, response, "Invalid JWT token");
        }
    }

    private void setErrorResponse(HttpStatus status, HttpServletResponse response, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ApiResponseEntity.createError 활용
        ApiResponseEntity<Object> errorResponse = ApiResponseEntity.error(null, message);
        String json = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(json);
    }
}