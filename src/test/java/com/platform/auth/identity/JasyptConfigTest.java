package com.platform.auth.identity;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.platform.auth.identity.common.config.JasyptConfig;

@SpringBootTest(classes = JasyptConfig.class)
class JasyptConfigTest {

	@Autowired
	private StringEncryptor jasyptStringEncryptor;

	@Test
	void jasypt_check_test() {
		// String plainText = "identity-service-1";
		String plainText = "password123!";

		// 1. 암호화
		String encryptedText = jasyptStringEncryptor.encrypt(plainText);
		System.out.println("Encrypted: " + encryptedText);

		// 2. 복호화
		String decryptedText = jasyptStringEncryptor.decrypt(encryptedText);
		System.out.println("Decrypted: " + decryptedText);

		// 3. 검증
		assertThat(decryptedText).isEqualTo(plainText);
	}
}