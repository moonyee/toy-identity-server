package com.platform.auth.identity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.platform.auth.identity.common.response.ApiResponseEntity;
import com.platform.auth.identity.controller.dto.JoinDto;
import com.platform.auth.identity.service.JoinService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth") // 기존 LoginController와 동일한 경로
@RequiredArgsConstructor
public class JoinController {

	private final JoinService joinService;

	// 1. 아이디 중복 검사
	@GetMapping("/check-id")
	public ApiResponseEntity<Boolean> checkId(@RequestParam String userId) {
		boolean isAvailable = joinService.isIdAvailable(userId);
		return ApiResponseEntity.success(isAvailable);
	}

	// 2. 이메일 중복 검사
	@GetMapping("/check-email")
	public ApiResponseEntity<Boolean> checkEmail(@RequestParam String email) {
		boolean isAvailable = joinService.isEmailAvailable(email);
		return ApiResponseEntity.success(isAvailable);
	}

	// 3. 회원가입 요청 (PENDING 상태 저장 및 인증 메일 발송 이벤트)
	@PostMapping("/join")
	public ApiResponseEntity<JoinDto.Response> join(@RequestBody JoinDto.Request request) {
		log.info("### Join attempt for user: {}, email: {}", request.getUserId(), request.getEmail());

		String savedUserId = joinService.join(request);

		return ApiResponseEntity.success(
			new JoinDto.Response(savedUserId, "인증 메일이 발송되었습니다. 이메일을 확인해주세요.")
		);
	}

	// 4. 이메일 인증 링크 클릭 시 호출될 API
	@GetMapping("/verify")
	public ApiResponseEntity<String> verify(@RequestParam String token) {
		joinService.verifyEmail(token);
		return ApiResponseEntity.success("이메일 인증이 완료되었습니다. 이제 로그인이 가능합니다.");
	}
}