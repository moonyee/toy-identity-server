package com.platform.auth.identity.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.auth.identity.common.response.ApiResponseEntity;
import com.platform.auth.identity.controller.dto.LoginDto;
import com.platform.auth.identity.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class LoginController {
	private final LoginService authService;

	@PostMapping("/login")
	public ApiResponseEntity<LoginDto.Response> login(@RequestBody LoginDto.Request request) {
		// 1. 비동기 Mono가 아닌 일반 객체를 바로 받습니다.
		String token = authService.login(request.getUserId(), request.getPassword());

		// 2. 응답 규격(ApiResponseEntity) 적용
		return ApiResponseEntity.success(new LoginDto.Response(token));
	}

	@PostMapping("/logout")
	public ApiResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
		// 1. SecurityContextHolder에서 인증 정보를 가져옵니다. (WebFlux의 exchange.getPrincipal() 대체)
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated()) {
			throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
		}

		String userId = auth.getName();
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		// 2. 헤더 검증
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Invalid Authorization header");
		}

		String token = authHeader.substring(7);
		log.info("### Logout attempt for user: {}, token: {}", userId, token.substring(0, 5) + "...");

		// 3. 서비스 호출 (동기 방식)
		boolean isDeleted = authService.logout(userId, token);

		if (isDeleted) {
			return ApiResponseEntity.success(Map.of("message", "Logged out successfully"));
		} else {
			// 실패 시나리오도 규격에 맞춰 응답
			return ApiResponseEntity.error(null, "Session not found or already expired");
		}
	}
}