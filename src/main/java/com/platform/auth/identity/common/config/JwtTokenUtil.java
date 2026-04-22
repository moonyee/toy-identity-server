package com.platform.auth.identity.common.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenUtil {

	private static final long ACCESS_TOKEN_TTL_MILLIS = 60L * 60L * 1000L; // 1시간

	private final SecretKey signingKey;

	public JwtTokenUtil(@Value("${jwt.secret}") String jwtSecret) {
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 권한 클레임을 포함한 액세스 토큰 발급.
	 * jti(JWT ID)를 부여해 Redis 세션 키 길이를 줄이고
	 * 동일 사용자의 다중 세션을 식별할 수 있게 한다.
	 */
	public String generateToken(String username, String role) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + ACCESS_TOKEN_TTL_MILLIS);

		return Jwts.builder()
			.setSubject(username)
			.setId(UUID.randomUUID().toString())
			.claim("role", role)
			.setIssuedAt(now)
			.setExpiration(expiration)
			.signWith(signingKey, SignatureAlgorithm.HS256)
			.compact();
	}
}
