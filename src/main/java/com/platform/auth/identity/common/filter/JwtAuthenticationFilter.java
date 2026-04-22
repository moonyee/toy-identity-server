package com.platform.auth.identity.common.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.identity.common.response.ApiResponseEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Identity 서비스(Servlet/MVC) JWT 인증 필터.
 * - 로그인/회원가입 등 인증 불필요 경로는 스킵
 * - SCryptKey 기반 HS256 검증
 * - Redis 세션 키 존재 검증
 * - 토큰의 role 클레임으로 권한 부여
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final SecretKey signingKey;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private static final String BEARER_PREFIX = "Bearer ";
	private static final List<String> SKIP_PATHS = List.of(
		"/auth/login",
		"/auth/join",
		"/auth/check-id",
		"/auth/check-email",
		"/auth/verify"
	);

	public JwtAuthenticationFilter(
		@Value("${jwt.secret}") String secretKey,
		StringRedisTemplate redisTemplate,
		ObjectMapper objectMapper
	) {
		this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
		// context-path(/api)를 제거한 경로로 화이트리스트 비교.
		// servletPath는 DispatcherServlet 매핑 방식에 따라 빈 문자열일 수 있어 사용하지 않는다.
		String matchPath = requestUri.startsWith(contextPath)
			? requestUri.substring(contextPath.length())
			: requestUri;
		log.debug("### Request path: {} (matchPath: {})", requestUri, matchPath);

		if (SKIP_PATHS.contains(matchPath)) {
			filterChain.doFilter(request, response);
			return;
		}

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
			log.warn("### No Authorization header or invalid format for path: {}", matchPath);
			setErrorResponse(HttpStatus.UNAUTHORIZED, response, "No valid authorization header");
			return;
		}

		String jwt = authHeader.substring(BEARER_PREFIX.length());

		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(signingKey)
				.build()
				.parseClaimsJws(jwt)
				.getBody();

			String username = claims.getSubject();
			String jti = claims.getId(); // JwtTokenUtil에서 부여한 jti
			String role = claims.get("role", String.class);
			if (role == null || role.isBlank()) {
				role = "ROLE_USER"; // 하위 호환
			}
			log.debug("### JWT parsed for user: {}, role: {}", username, role);

			// Redis 세션 키 검증
			String redisKey = "AUTH:" + username + ":" + (jti != null ? jti : jwt);
			if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
				log.warn("### Session not found in Redis for user: {}", username);
				setErrorResponse(HttpStatus.UNAUTHORIZED, response, "Session expired or logged out");
				return;
			}

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				username, null, Collections.singletonList(new SimpleGrantedAuthority(role))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			filterChain.doFilter(request, response);

		} catch (ExpiredJwtException e) {
			log.warn("### JWT token is expired: {}", e.getMessage());
			setErrorResponse(HttpStatus.UNAUTHORIZED, response, "JWT token is expired");
		} catch (Exception e) {
			log.warn("### Invalid JWT token or filter error: {}", e.getMessage());
			setErrorResponse(HttpStatus.UNAUTHORIZED, response, "Invalid JWT token");
		}
	}

	private void setErrorResponse(HttpStatus status, HttpServletResponse response, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponseEntity<Object> errorResponse = ApiResponseEntity.error(null, message);
		String json = objectMapper.writeValueAsString(errorResponse);

		response.getWriter().write(json);
	}
}
