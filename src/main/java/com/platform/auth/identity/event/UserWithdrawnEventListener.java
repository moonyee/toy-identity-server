package com.platform.auth.identity.event;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 탈퇴 AFTER_COMMIT 리스너.
 *
 * <p>세션 및 이메일 인증 코드 Redis 키를 일괄 삭제한다. 동기(@Async 없음) 실행 —
 * 세션 무효화가 탈퇴 응답보다 먼저 완료되어야 다른 기기의 기존 JWT가 즉시 차단된다.
 * Redis DEL 수회는 수십 ms 이내라 사용자 응답 지연에 큰 영향이 없다.
 *
 * <p>실패 시 로그만 남기고 삼킨다 — DB 커밋은 이미 완료되었으므로 탈퇴 자체를 되돌릴 수 없고,
 * 잔여 세션은 TTL(3600초)로 자연 소멸한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawnEventListener {

    private static final String SESSION_LIST_KEY_PREFIX = "USER_SESSIONS:";
    private static final String AUTH_KEY_PREFIX = "AUTH:";
    private static final String CODE_KEY_PREFIX = "auth:code:";

    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        try {
            String sessionListKey = SESSION_LIST_KEY_PREFIX + event.getUserId();
            Set<String> jtis = redisTemplate.opsForSet().members(sessionListKey);
            if (jtis != null && !jtis.isEmpty()) {
                List<String> authKeys = jtis.stream()
                    .map(jti -> AUTH_KEY_PREFIX + event.getUserId() + ":" + jti)
                    .toList();
                redisTemplate.delete(authKeys);
            }
            redisTemplate.delete(sessionListKey);

            if (event.getEmail() != null && !event.getEmail().isBlank()) {
                redisTemplate.delete(CODE_KEY_PREFIX + event.getEmail());
            }

            log.info("### Redis session cleanup done after withdraw userId={}", event.getUserId());
        } catch (Exception e) {
            log.warn("### Redis session cleanup failed after withdraw userId={}, err={}",
                event.getUserId(), e.getMessage(), e);
        }
    }
}
