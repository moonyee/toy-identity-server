package com.platform.auth.identity.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.auth.identity.domain.entity.User;
import com.platform.auth.identity.domain.entity.UserStatus;
import com.platform.auth.identity.domain.repository.UserRepository;
import com.platform.auth.identity.event.UserWithdrawnEvent;
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 처리 서비스.
 *
 * <p>플로우:
 * <ol>
 *   <li>사용자 조회 → 이미 DELETED면 존재 은폐 ({@link ErrorCode#USER_NOT_FOUND})</li>
 *   <li>비밀번호 재확인 → 불일치 시 {@link ErrorCode#PASSWORD_MISMATCH}</li>
 *   <li>{@link User#withdraw()} — status=DELETED, withdrawnAt=now</li>
 *   <li>{@link UserWithdrawnEvent} 발행 → {@code @TransactionalEventListener(AFTER_COMMIT)} 가
 *       Redis 세션·인증 코드 정리</li>
 * </ol>
 *
 * <p>메서드 전체가 {@code @Transactional} 이며 Redis 부수 작업은 AFTER_COMMIT 리스너에 위임해
 * DB/Redis 정합성을 확보한다. 이 패턴은 {@code JoinService.join()} 의 메일 발송 플로우와 동일.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void withdraw(String userId, String rawPassword) {
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴된 계정 — 존재 은폐 목적으로 USER_NOT_FOUND.
        // 인증 세션이 레이스 상황에서 살아있는 경우 방어적으로 동작한다.
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ErrorException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ErrorException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.withdraw();
        eventPublisher.publishEvent(new UserWithdrawnEvent(user.getUserId(), user.getEmail()));

        log.info("### User withdrawn userId={}", user.getUserId());
    }
}
