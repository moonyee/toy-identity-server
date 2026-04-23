package com.platform.auth.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.auth.identity.domain.entity.User;
import com.platform.auth.identity.domain.entity.UserStatus;
import com.platform.auth.identity.domain.repository.UserRepository;
import com.platform.auth.identity.event.UserRegisteredEvent;
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 인증 코드 검증 및 재발송 서비스.
 *
 * <p>Redis 키 계약:
 * <pre>
 *   auth:code:{email} = "{6자리 코드}"   TTL 180초
 * </pre>
 * 이 키 포맷은 {@code JoinService}와 공유하는 프로젝트 계약이다 (CLAUDE.md 규약).
 *
 * <p>검증 흐름:
 * <ol>
 *   <li>Redis 조회 → null 이면 {@code VERIFICATION_CODE_EXPIRED}</li>
 *   <li>{@link MessageDigest#isEqual(byte[], byte[])}로 상수시간 비교</li>
 *   <li>일치 시 User를 조회해 {@link User#activate()}, Redis 키 삭제</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private static final String CODE_KEY_PREFIX = "auth:code:";

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.verification.code-ttl-seconds:180}")
    private long codeTtlSeconds;

    @Transactional
    public void verifyCode(String email, String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            throw new ErrorException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        String key = CODE_KEY_PREFIX + email;
        String saved = redisTemplate.opsForValue().get(key);
        if (saved == null) {
            // 만료 또는 미발급. 사용자 존재 여부는 노출하지 않는다.
            throw new ErrorException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!constantTimeEquals(saved, code)) {
            throw new ErrorException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ErrorException(ErrorCode.VERIFICATION_CODE_INVALID));

        if (user.getStatus() == UserStatus.ACTIVE) {
            // 이미 인증된 계정이면 유효한 코드가 남아있을 수 없도록 키만 정리하고 idempotent 처리.
            redisTemplate.delete(key);
            throw new ErrorException(ErrorCode.MAIL_ALREADY_VERIFIED);
        }

        user.activate();
        redisTemplate.delete(key);
        log.info("### Email verified: userId={}", user.getUserId());
    }

    /**
     * 코드 재발송. 기존 키를 덮어쓰므로 마지막 코드만 유효하다.
     * rate-limit은 후속 Phase에서 Token Bucket으로 추가한다.
     *
     * <p>{@code @Transactional} 이 필수다 — 리스너가
     * {@code @TransactionalEventListener(AFTER_COMMIT)} 로 등록되어 있어
     * 활성 트랜잭션 없이 publish 하면 이벤트가 조용히 무시된다. DB 쓰기는 없지만
     * 트랜잭션 경계가 있어야 AFTER_COMMIT 콜백이 실행된다.
     */
    @Transactional
    public void resendCode(String email) {
        if (email == null || email.isBlank()) {
            throw new ErrorException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 탈퇴한 계정은 존재하지 않는 것처럼 은폐 — 이메일 enumeration 방어.
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ErrorException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ErrorException(ErrorCode.MAIL_ALREADY_VERIFIED);
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, Duration.ofSeconds(codeTtlSeconds));
        eventPublisher.publishEvent(new UserRegisteredEvent(email, code));
        log.info("### Verification code resent userId={}", user.getUserId());
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
