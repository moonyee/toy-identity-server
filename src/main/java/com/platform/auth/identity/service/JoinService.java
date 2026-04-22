package com.platform.auth.identity.service;

import java.time.Duration;
import java.util.UUID;

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

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	// JoinService는 단순 String key/value만 다루므로 StringRedisTemplate으로 충분.
	private final StringRedisTemplate redisTemplate;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public String join(JoinDto.Request request) {
		if (userRepository.existsByUserId(request.getUserId())) {
			throw new ErrorException(ErrorCode.USER_NOT_FOUND, "이미 존재하는 아이디입니다.");
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

		String authToken = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set("EMAIL_AUTH:" + authToken, user.getUserId(), Duration.ofMinutes(30));

		// TODO: UserRegisteredEventListener / MailService가 빈 껍데기 상태이므로
		// 실제 이메일은 발송되지 않는다. 메일 기능은 spring-boot-starter-mail로 완성하거나
		// 명세에서 제외할지 결정 필요.
		eventPublisher.publishEvent(new UserRegisteredEvent(user.getEmail(), authToken));

		return user.getUserId();
	}

	@Transactional
	public void verifyEmail(String token) {
		String key = "EMAIL_AUTH:" + token;
		String userId = redisTemplate.opsForValue().get(key);

		if (userId == null) {
			throw new ErrorException(ErrorCode.UNAUTHORIZED, "만료되거나 유효하지 않은 인증 토큰입니다.");
		}

		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

		user.activate();
		redisTemplate.delete(key);
	}

	public boolean isIdAvailable(String userId) {
		return !userRepository.existsByUserId(userId);
	}

	public boolean isEmailAvailable(String email) {
		return !userRepository.existsByEmail(email);
	}
}
