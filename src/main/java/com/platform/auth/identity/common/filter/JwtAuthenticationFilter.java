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
	// SecurityConfig.permitAll 목록과 반드시 동일해야 한다. (context-path `/api`가 제거된 기준)
	// Swagger UI 관련 경로는 prefix 매칭으로 별도 처리(아래 SKIP_PREFIXES)
	private static final List<String> SKIP_PATHS = List.of(
		"/auth/login",
		"/auth/join",
		"/auth/check-id",
		"/auth/check-email",
		"/v1/auth/verify",
		"/v1/auth/resend",
		// `/v3/api-docs` 는 Swagger UI가 최초로 요청하는 OpenAPI JSON 본체.
		// prefix `/v3/api-docs/` 만으로는 이 경로 자체를 못 잡으므로 exact 매칭에 반드시 포함.
		"/v3/api-docs",
		"/v3/api-docs.yaml",
		"/swagger-ui.html"
	);
	// prefix 매칭. `/swagger-ui/*`, `/v3/api-docs/*` 하위 리소스 모두 통과시킨다.
	// (swagger-config, 페이지 번들·CSS 등)
	private static final List<String> SKIP_PREFIXES = List.of(
		"/swagger-ui/",
		"/v3/api-docs/"
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

		if (SKIP_PATHS.contains(matchPath) || matchesSkipPrefix(matchPath)) {
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

	private boolean matchesSkipPrefix(String path) {
		for (String prefix : SKIP_PREFIXES) {
			if (path.startsWith(prefix)) return true;
		}
		return false;
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
