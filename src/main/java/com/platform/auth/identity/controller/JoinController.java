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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "Auth - Join", description = "회원가입 / 아이디·이메일 중복 검사")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class JoinController {

	private final JoinService joinService;

	@Operation(
		summary = "아이디 중복 검사",
		description = "사용 가능한 아이디이면 true, 이미 사용 중이면 false."
	)
	@ApiResponses(@ApiResponse(responseCode = "200", description = "검사 결과"))
	@GetMapping("/check-id")
	public ApiResponseEntity<Boolean> checkId(
		@Parameter(description = "검사할 아이디", example = "alice") @RequestParam String userId
	) {
		boolean isAvailable = joinService.isIdAvailable(userId);
		return ApiResponseEntity.success(isAvailable);
	}

	@Operation(
		summary = "이메일 중복 검사",
		description = "사용 가능한 이메일이면 true, 이미 사용 중이면 false."
	)
	@ApiResponses(@ApiResponse(responseCode = "200", description = "검사 결과"))
	@GetMapping("/check-email")
	public ApiResponseEntity<Boolean> checkEmail(
		@Parameter(description = "검사할 이메일", example = "alice@example.com") @RequestParam String email
	) {
		boolean isAvailable = joinService.isEmailAvailable(email);
		return ApiResponseEntity.success(isAvailable);
	}

	@Operation(
		summary = "회원가입",
		description = "사용자를 PENDING 상태로 저장하고 6자리 인증 코드를 이메일로 발송한다. 사용자는 3분 내 `/api/v1/auth/verify`로 코드를 검증해야 로그인 가능한 ACTIVE 상태가 된다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "가입 성공, 인증 메일 발송 큐에 투입"),
		@ApiResponse(responseCode = "409", description = "아이디 또는 이메일 중복")
	})
	@PostMapping("/join")
	public ApiResponseEntity<JoinDto.Response> join(@RequestBody JoinDto.Request request) {
		log.info("### Join attempt for user: {}", request.getUserId());

		String savedUserId = joinService.join(request);

		return ApiResponseEntity.success(
			new JoinDto.Response(savedUserId, "인증 메일이 발송되었습니다. 3분 내에 인증 코드를 입력해 주세요.")
		);
	}
}
