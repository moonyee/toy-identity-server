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
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;
import com.platform.auth.identity.service.LoginService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Auth - Login", description = "로그인 / 로그아웃 엔드포인트")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {
	private final LoginService authService;

	@Operation(
		summary = "로그인",
		description = "아이디·비밀번호로 로그인하여 JWT access token을 발급받는다. 동시 세션 한도(기본 3)를 초과하면 409로 거부된다. 이메일 인증이 완료되지 않은 PENDING 계정은 403 EMAIL_NOT_VERIFIED. 탈퇴 계정은 존재 은폐를 위해 401 USER_NOT_FOUND."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공, access token 반환"),
		@ApiResponse(responseCode = "401", description = "아이디 혹은 비밀번호 불일치 / 탈퇴 계정"),
		@ApiResponse(responseCode = "403", description = "이메일 미인증(PENDING 상태)"),
		@ApiResponse(responseCode = "409", description = "동시 세션 한도 초과")
	})
	@PostMapping("/login")
	public ApiResponseEntity<LoginDto.Response> login(@RequestBody LoginDto.Request request) {
		// 1. 비동기 Mono가 아닌 일반 객체를 바로 받습니다.
		String token = authService.login(request.getUserId(), request.getPassword());

		// 2. 응답 규격(ApiResponseEntity) 적용
		return ApiResponseEntity.success(new LoginDto.Response(token));
	}

	@Operation(
		summary = "로그아웃",
		description = "현재 access token의 jti에 해당하는 Redis 세션을 삭제해 즉시 무효화한다. 다른 기기의 세션은 영향받지 않는다."
	)
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공 또는 이미 만료된 세션"),
		@ApiResponse(responseCode = "401", description = "인증 정보 없음 / 유효하지 않은 토큰")
	})
	@PostMapping("/logout")
	public ApiResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
		// 1. SecurityContextHolder에서 인증 정보를 가져옵니다. (WebFlux의 exchange.getPrincipal() 대체)
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated()) {
			throw new ErrorException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다.");
		}

		String userId = auth.getName();
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		// 2. 헤더 검증
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new ErrorException(ErrorCode.UNAUTHORIZED, "Invalid Authorization header");
		}

		String token = authHeader.substring(7);
		// 토큰 전체를 절대 로그에 남기지 않는다. prefix 일부만 남겨 디버깅용으로 사용.
		log.info("### Logout attempt for user: {}, token-prefix: {}...", userId, token.substring(0, Math.min(5, token.length())));

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
