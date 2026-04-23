package com.platform.auth.identity.service;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 발송 실패를 로그로만 기록하는 기본 핸들러. 운영 배포 전에는
 * Redis DLQ 기반 {@code RedisMailFailureHandler} 로 교체하여 재시도 가능성을 확보해야 한다.
 *
 * <p>코드 원문은 로그에 남기지 않고 prefix 2자리까지만 기록한다.
 */
@Slf4j
@Component
public class LogOnlyMailFailureHandler implements MailSendFailureHandler {

    @Override
    public void handle(String email, String code, Throwable cause) {
        String codeHint = code == null || code.length() < 2 ? "??"
            : code.substring(0, 2) + "****";
        // 스택트레이스 전체를 남겨 SMTP 에러 원인(535 auth fail, connection timeout, SSL handshake 등) 진단을 돕는다.
        log.error("### MAIL-DLQ candidate: email={}, codeHint={}, cause={}",
            maskEmail(email), codeHint, cause.toString(), cause);
    }

    private String maskEmail(String email) {
        if (email == null) return "unknown";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String localMasked = local.length() <= 2 ? local.charAt(0) + "*"
            : local.substring(0, 2) + "*".repeat(Math.max(1, local.length() - 2));
        return localMasked + domain;
    }
}
