package com.platform.auth.identity.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원가입이 DB에 PENDING 상태로 커밋된 직후 발행되는 이벤트.
 * 실제 처리(메일 발송)는 {@code @TransactionalEventListener(AFTER_COMMIT)} 로 수신되어
 * 트랜잭션 롤백 시 메일이 나가지 않도록 보장한다.
 */
@Getter
@RequiredArgsConstructor
public class UserRegisteredEvent {
    private final String email;
    private final String code;
}
