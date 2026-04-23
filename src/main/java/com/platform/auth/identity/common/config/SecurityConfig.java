package com.platform.auth.identity.common.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.platform.auth.identity.common.filter.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			// context-path(/api)가 적용된 후 매처는 /api 가 잘려나간 경로로 비교한다.
			// v1 인증 코드 엔드포인트는 별도 prefix(/v1/auth/*)로 분리되어 있으며
			// Gateway 쪽 permit 목록과 반드시 동기화되어야 한다.
			// Swagger UI / OpenAPI JSON 은 개발 편의상 permit. 운영 profile 에서는 별도 차단 정책으로 제어.
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/auth/login",
					"/auth/join",
					"/auth/check-id",
					"/auth/check-email",
					"/v1/auth/verify",
					"/v1/auth/resend",
					// Swagger / OpenAPI. `/v3/api-docs/**` 는 하위만 매칭해서 `/v3/api-docs` 본체를 놓치므로
					// 정확 매처를 별도 등록한다. (JwtAuthenticationFilter 의 SKIP_PATHS 와 동일 이유)
					"/v3/api-docs",
					"/v3/api-docs/**",
					"/v3/api-docs.yaml",
					"/swagger-ui/**",
					"/swagger-ui.html"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	/**
	 * 확장 가능한 패스워드 해싱 구성.
	 *
	 * <p>해시 앞에 알고리즘 식별자({@code {bcrypt}}, {@code {argon2}} 등)를 붙여 저장하므로
	 * 운영 중에 무중단으로 알고리즘을 업그레이드할 수 있다.
	 * <ul>
	 *   <li>신규 해시는 {@code idForEncode}로 지정된 알고리즘(기본: bcrypt strength 12)으로 저장</li>
	 *   <li>기존에 저장된 다른 알고리즘의 해시도 {@code encoders} 맵에 등록돼 있으면 그대로 검증</li>
	 *   <li>Argon2 / SCrypt는 런타임에 BouncyCastle이 필요(build.gradle에 bcprov-jdk18on 추가 완료)</li>
	 * </ul>
	 *
	 * <p>나중에 Argon2로 전환하고 싶다면 {@code idForEncode}만 {@code "argon2"}로 바꾸면 된다.
	 * 이미 저장된 {@code {bcrypt}} 해시는 로그인 시 자동으로 검증되고,
	 * 새 가입자/비밀번호 변경자부터는 {@code {argon2}} 해시로 저장된다.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		String idForEncode = "bcrypt";

		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put("bcrypt", new BCryptPasswordEncoder(12));
		encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
		encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
		encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());

		DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(idForEncode, encoders);
		// prefix 없는 레거시 해시(기존에 bcrypt 기본값으로 저장된 값)도 그대로 검증 가능하도록 지정.
		encoder.setDefaultPasswordEncoderForMatches(encoders.get("bcrypt"));
		return encoder;
	}
}
