package com.platform.auth.identity.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.platform.auth.identity.common.config.JwtTokenUtil;
import com.platform.auth.identity.domain.entity.User;
import com.platform.auth.identity.domain.repository.UserRepository;
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
	@Value("${auth.max-session:3}") // 설정 파일(yml)에서 관리
	private int MAX_SESSION;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenUtil jwtTokenUtil;
	private final StringRedisTemplate redisTemplate;

	public String login(String userId, String rawPassword) {
		User user = userRepository.findByUserId(userId)
			.orElseThrow(
			() -> new ErrorException(ErrorCode.USER_NOT_FOUND)
		);

		// 2. 비밀번호 검증
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new RuntimeException("Invalid password");
		}

		String sessionListKey = "USER_SESSIONS:" + userId;

		// 3. 현재 세션 리스트 크기 확인 (동기)
		Long size = redisTemplate.opsForSet().size(sessionListKey);
		if (size != null && size >= MAX_SESSION) {
			throw new RuntimeException("최대 " + MAX_SESSION + "대까지만 접속 가능합니다.");
		}

		// 4. 토큰 생성 및 Redis 저장
		String token = jwtTokenUtil.generateToken(user.getUserId());
		String sessionKey = "AUTH:" + userId + ":" + token;

		// 5. 상세 세션 저장 및 세션 리스트 추가 (순차 실행)
		// opsForValue().set()은 리턴값이 void입니다. 에러가 나면 예외가 터지므로 if 체크가 불필요합니다.
		redisTemplate.opsForValue().set(sessionKey, token, Duration.ofHours(1));

		// Set 리스트에 추가
		redisTemplate.opsForSet().add(sessionListKey, token);
		// 리스트 키 자체도 만료 시간을 관리해주는 것이 좋습니다 (예: 1시간)
		redisTemplate.expire(sessionListKey, Duration.ofHours(1));

		log.info("### Redis 세션 저장 및 리스트 추가 완료: {}", userId);

		return token;
	}

	/**
	 * 로그아웃 로직 (MVC 버전)
	 */
	public boolean logout(String userId, String token) {
		String sessionKey = "AUTH:" + userId + ":" + token;
		String sessionListKey = "USER_SESSIONS:" + userId;

		// 1. 개별 세션 키 삭제
		Boolean isDeleted = redisTemplate.delete(sessionKey);

		// 2. 세션 리스트에서 해당 토큰 제거
		redisTemplate.opsForSet().remove(sessionListKey, token);

		if (Boolean.TRUE.equals(isDeleted)) {
			log.info("### Redis 삭제 완료: {}", sessionKey);
			return true;
		} else {
			log.warn("### Redis 삭제 실패 (키 없음): {}", sessionKey);
			return false;
		}
	}
}