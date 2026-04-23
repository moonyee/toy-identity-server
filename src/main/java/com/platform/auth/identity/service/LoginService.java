package com.platform.auth.identity.service;

import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.platform.auth.identity.common.config.JwtTokenUtil;
import com.platform.auth.identity.domain.entity.User;
import com.platform.auth.identity.domain.entity.UserStatus;
import com.platform.auth.identity.domain.repository.UserRepository;
import com.platform.auth.identity.exception.ErrorCode;
import com.platform.auth.identity.exception.ErrorException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

	@Value("${auth.max-session:3}")
	private int maxSession;

	@Value("${jwt.secret}")
	private String jwtSecret;

	private SecretKey signingKey;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenUtil jwtTokenUtil;
	private final StringRedisTemplate redisTemplate;

	/**
	 * 동시 로그인 세션 등록을 원자적으로 처리하는 Lua 스크립트.
	 * - KEYS[1] = USER_SESSIONS:{userId}      (Set, 활성 jti 모음)
	 * - KEYS[2] = AUTH:{userId}:{jti}         (개별 세션 키)
	 * - ARGV[1] = jti
	 * - ARGV[2] = TTL(초)
	 * - ARGV[3] = MAX_SESSION
	 * - 반환값 1 = 등록 성공, 0 = 한도 초과
	 *
	 * 단순 SCARD → SADD 패턴은 동시에 다수 로그인이 들어오면 한도를 넘길 수 있어
	 * Lua로 원자화한다.
	 */
	private static final String LIMIT_SCRIPT = """
		local current = redis.call('SCARD', KEYS[1])
		if current >= tonumber(ARGV[3]) then
		    return 0
		end
		redis.call('SADD', KEYS[1], ARGV[1])
		redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
		redis.call('SET', KEYS[2], ARGV[1], 'EX', tonumber(ARGV[2]))
		return 1
		""";

	private final RedisScript<Long> limitScript =
		new DefaultRedisScript<>(LIMIT_SCRIPT, Long.class);

	@PostConstruct
	void initSigningKey() {
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public String login(String userId, String rawPassword) {
		User user = userRepository.findByUserId(userId)
			.orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

		// 탈퇴한 계정은 존재하지 않는 것처럼 은폐한다.
		// 비밀번호 검증 이전에 차단하여 DELETED 상태의 해시가 공격자에게 노출되지 않게 한다.
		if (user.getStatus() == UserStatus.DELETED) {
			throw new ErrorException(ErrorCode.USER_NOT_FOUND);
		}

		// 비밀번호 불일치 시에도 사용자 존재 여부를 노출하지 않기 위해 동일한 에러 코드를 사용
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new ErrorException(ErrorCode.USER_NOT_FOUND);
		}

		// 이메일 인증이 완료되지 않은 계정은 로그인을 거부.
		// 비밀번호 검증을 통과한 이후에만 이 분기에 도달하므로, 공격자가 계정 존재 여부를 탐지하는 수단이 되지 않는다.
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ErrorException(ErrorCode.EMAIL_NOT_VERIFIED);
		}

		String role = user.getRole() != null ? user.getRole().name() : "ROLE_USER";
		String token = jwtTokenUtil.generateToken(user.getUserId(), role);
		String jti = parseJti(token);

		String sessionListKey = "USER_SESSIONS:" + userId;
		String sessionKey = "AUTH:" + userId + ":" + jti;

		Long ok = redisTemplate.execute(
			limitScript,
			List.of(sessionListKey, sessionKey),
			jti,
			String.valueOf(Duration.ofHours(1).toSeconds()),
			String.valueOf(maxSession)
		);

		if (ok == null || ok == 0L) {
			throw new ErrorException(ErrorCode.MAX_SESSION_EXCEEDED,
				"최대 " + maxSession + "대까지만 접속 가능합니다.");
		}

		log.info("### Redis 세션 저장 완료: user={}, jti={}", userId, jti);
		return token;
	}

	public boolean logout(String userId, String token) {
		String jti = parseJti(token);
		String sessionKey = "AUTH:" + userId + ":" + jti;
		String sessionListKey = "USER_SESSIONS:" + userId;

		Boolean isDeleted = redisTemplate.delete(sessionKey);
		redisTemplate.opsForSet().remove(sessionListKey, jti);

		if (Boolean.TRUE.equals(isDeleted)) {
			log.info("### Redis 세션 삭제 완료: {}", sessionKey);
			return true;
		}
		log.warn("### Redis 세션 삭제 실패(키 없음): {}", sessionKey);
		return false;
	}

	private String parseJti(String token) {
		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(signingKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
			String jti = claims.getId();
			return jti != null ? jti : token; // 하위 호환: jti가 없으면 토큰 자체 사용
		} catch (Exception e) {
			// 토큰 파싱 실패 시에도 키 일관성을 깨지 않도록 토큰 자체를 식별자로 사용
			log.debug("Failed to parse jti, falling back to token", e);
			return token;
		}
	}
}
