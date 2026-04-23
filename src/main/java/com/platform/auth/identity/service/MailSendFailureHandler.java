package com.platform.auth.identity.service;

/**
 * 메일 발송 실패 처리 확장 포인트. 현재 구현은 {@link LogOnlyMailFailureHandler} 뿐이나,
 * 향후 Redis Stream 기반 DLQ({@code mail:dlq:*}) + 재시도 워커 구현체로 교체하기 위해
 * 인터페이스로 추출해 둔다.
 */
public interface MailSendFailureHandler {
    void handle(String email, String code, Throwable cause);
}
