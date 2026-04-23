package com.platform.auth.identity.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 탈퇴가 DB에 커밋된 직후 발행되는 이벤트.
 *
 * <p>실제 부수 작업(Redis 세션·인증 코드 정리, 향후 감사 로그·데이터 익명화)은
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 가 수신해 실행한다.
 * 트랜잭션이 롤백되면 이 이벤트의 부수 작업도 실행되지 않으므로 DB 상태와 Redis 상태가 꼬이지 않는다.
 */
@Getter
@RequiredArgsConstructor
public class UserWithdrawnEvent {
    private final String userId;
    private final String email;
}
