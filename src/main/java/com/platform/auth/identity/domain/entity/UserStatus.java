package com.platform.auth.identity.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
	PENDING("대기", "이메일 인증 전"),
	ACTIVE("활성화", "정상 사용 가능"),
	DELETED("삭제", "탈퇴 계정");

	private final String title;
	private final String description;
}