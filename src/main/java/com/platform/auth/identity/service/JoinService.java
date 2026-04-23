package com.platform.auth.identity.service;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.auth.identity.controller.dto.JoinDto;
import com.platform.auth.identity.domain.entity.User;
import com.platform.auth.identity.domain.entity.UserRole;
import com.platform.auth.identity.domain.entity.UserStatus;
import com.platform.auth.identity.domain.repository.UserRepository;
import com.platform.auth.identity.event.UserRegisteredEvent;
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JoinService {

    private static final String CODE_KEY_PREFIX = "auth:code:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.verification.code-ttl-seconds:180}")
    private long codeTtlSeconds;

    /**
     * 회원가입 플로우:
     * <ol>
     *   <li>아이디/이메일 중복 검사</li>
     *   <li>PENDING 상태로 User 저장 (해시된 비밀번호)</li>
     *   <li>6자리 코드 생성 → {@code auth:code:{email}} 에 TTL 180초로 저장</li>
     *   <li>{@link UserRegisteredEvent} 발행 — {@code AFTER_COMMIT} 후 가상 스레드로 메일 발송</li>
     * </ol>
     * 트랜잭션이 롤백되면 Redis 키·메일 모두 발생하지 않도록
     * Redis write는 저장 직후, publish는 리스너가 AFTER_COMMIT 시점에 동작.
     *
     * <p>참고: Redis write 자체는 트랜잭션 바깥이라 이론적으로 "DB 실패 시 Redis 잔여"가 가능하나,
     * 코드는 어차피 TTL 180초로 자동 소멸하고, 사용자가 활성화되지 않은 email로 로그인도 막혀 있다.
     * 이 수준의 일관성이면 충분 — Saga/2PC까지는 도입하지 않는다.
     */
    @Transactional
    public String join(JoinDto.Request request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new ErrorException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ErrorException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
            .userId(request.getUserId())
            .password(passwordEncoder.encode(request.getPassword()))
            .userName(request.getUserName())
            .email(request.getEmail())
            .status(UserStatus.PENDING)
            .role(UserRole.ROLE_USER)
            .build();
        userRepository.save(user);

        String code = generateCode();
        redisTemplate.opsForValue().set(
            CODE_KEY_PREFIX + request.getEmail(),
            code,
            Duration.ofSeconds(codeTtlSeconds)
        );

        eventPublisher.publishEvent(new UserRegisteredEvent(request.getEmail(), code));

        return user.getUserId();
    }

    public boolean isIdAvailable(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
