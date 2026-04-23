package com.platform.auth.identity.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.auth.identity.common.response.ApiResponseEntity;
import com.platform.auth.identity.controller.dto.VerifyDto;
import com.platform.auth.identity.service.VerifyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * v1 인증 코드 기반 이메일 검증 엔드포인트.
 * <p>
 * Gateway의 라우트 {@code /api/v1/auth/**} → identity:8182 로 매핑되며,
 * context-path {@code /api}가 적용되어 내부 매핑은 {@code /v1/auth/...} 가 된다.
 * </p>
 * <p>
 * 구 URL 토큰 플로우({@code GET /auth/verify?token=})는 폐기되었고,
 * 이 컨트롤러의 {@code POST /v1/auth/verify}가 이를 대체한다.
 * </p>
 */
@Slf4j
@Tag(name = "Auth - Verify", description = "이메일 인증 코드 검증 / 재발송")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class VerifyController {

    private final VerifyService verifyService;

    @Operation(
        summary = "인증 코드 검증",
        description = "이메일로 발송된 6자리 코드를 검증한다. 성공 시 사용자 상태가 ACTIVE로 전이되어 로그인 가능."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인증 성공"),
        @ApiResponse(responseCode = "401", description = "코드 만료 또는 불일치"),
        @ApiResponse(responseCode = "409", description = "이미 인증된 계정")
    })
    @PostMapping("/verify")
    public ApiResponseEntity<String> verify(@RequestBody VerifyDto.VerifyRequest request) {
        verifyService.verifyCode(request.getEmail(), request.getCode());
        return ApiResponseEntity.success("이메일 인증이 완료되었습니다. 로그인이 가능합니다.");
    }

    @Operation(
        summary = "인증 코드 재발송",
        description = "기존 코드를 덮어쓰고 새 6자리 코드를 생성·발송한다. TTL 180초로 초기화."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "재발송 성공(비동기 메일 큐 투입)"),
        @ApiResponse(responseCode = "401", description = "존재하지 않는 이메일 또는 탈퇴 계정"),
        @ApiResponse(responseCode = "409", description = "이미 인증된 계정")
    })
    @PostMapping("/resend")
    public ApiResponseEntity<String> resend(@RequestBody VerifyDto.ResendRequest request) {
        verifyService.resendCode(request.getEmail());
        return ApiResponseEntity.success("인증 메일을 재발송했습니다.");
    }
}
