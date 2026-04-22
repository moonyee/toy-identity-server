package com.platform.auth.identity.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@Column(name = "user_id")
	private String userId;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String userName;

	@Column(nullable = false, unique = true)
	private String email; // 통으로 저장

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status; // PENDING, ACTIVE 등 상태 관리

	@Enumerated(EnumType.STRING)
	private UserRole role;

	@Builder
	public User(String userId, String password, String userName, String email, UserStatus status, UserRole role) {
		this.userId = userId;
		this.password = password;
		this.userName = userName;
		this.email = email;
		this.status = status;
		this.role = role;
	}

	// 인증 완료 시 상태를 변경하는 비즈니스 메서드
	public void activate() {
		this.status = UserStatus.ACTIVE;
	}
}
