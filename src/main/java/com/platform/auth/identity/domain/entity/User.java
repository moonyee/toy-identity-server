package com.platform.auth.identity.domain.entity;

import java.time.LocalDateTime;

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

	// 탈퇴 시각. null 이면 탈퇴하지 않은 계정. DELETED 상태 전이 시에만 기록된다.
	@Column(name = "withdrawn_at")
	private LocalDateTime withdrawnAt;

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

	/**
	 * 회원 탈퇴. Soft delete — row를 유지하고 상태만 {@link UserStatus#DELETED}로 전이한다.
	 * 후속 hard-delete 배치가 {@code withdrawnAt} 기준 유예 기간을 지난 계정을 정리한다.
	 * 비밀번호·개인정보 익명화는 배치에서 별도 수행.
	 */
	public void withdraw() {
		this.status = UserStatus.DELETED;
		this.withdrawnAt = LocalDateTime.now();
	}
}
