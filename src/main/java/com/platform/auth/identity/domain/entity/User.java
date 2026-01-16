package com.platform.auth.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String userId; // 로그인 ID

	@Column(nullable = false)
	private String userName; // 사용자 이름

	@Column(nullable = false)
	private String password; // BCrypt 암호화된 비밀번호

	private String role; // ROLE_USER 등

	@Builder
	public User(String userId, String password, String role, String userName) {
		this.userId = userId;
		this.password = password;
		this.role = role;
		this.userName = userName;
	}
}
