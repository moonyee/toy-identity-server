package com.platform.auth.identity.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.platform.auth.identity.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUserId(String userId);
}
