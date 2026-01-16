package com.platform.auth.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class bCryptTest {

	@Test
	void bcrypt_test() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		System.out.println(encoder.encode("user1"));
		System.out.println(encoder.encode("user2"));
		System.out.println(encoder.encode("user3"));
		System.out.println(encoder.encode("user4"));
		System.out.println(encoder.encode("user5"));
		System.out.println(encoder.encode("user6"));
		System.out.println(encoder.encode("user7"));
		System.out.println(encoder.encode("user8"));
		System.out.println(encoder.encode("user9"));
		System.out.println(encoder.encode("user10"));
		System.out.println(encoder.encode("user11"));
		System.out.println(encoder.encode("user12"));
		System.out.println(encoder.encode("user13"));
		System.out.println(encoder.encode("password1!"));
	}
}
