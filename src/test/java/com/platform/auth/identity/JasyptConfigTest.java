package com.platform.auth.identity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.platform.auth.identity.common.config.JasyptConfig;

/**
 * Jasypt 암호화 자가검증 / 운영 ENC 값 재발급용 유틸리티 테스트.
 *
 * 실행 전 반드시 실제 운영과 동일한 마스터 비밀번호를 환경변수/시스템 프로퍼티로 주입한다.
 *   ./gradlew test --tests JasyptConfigTest \
 *       -Djasypt.encryptor.password=<운영에서_쓰는_마스터_키>
 *
 * 그래야 여기서 출력된 ENC 값을 application.yml에 그대로 붙여 넣었을 때
 * 애플리케이션이 동일 키로 복호화할 수 있다.
 */
@SpringBootTest(classes = JasyptConfig.class)
@TestPropertySource(properties = "jasypt.encryptor.password=test-only-password-do-not-use-in-prod")
class JasyptConfigTest {

	@Autowired
	private StringEncryptor jasyptStringEncryptor;

	@Test
	void jasypt_check_test() {
		String plainText = "password123!";
		String encryptedText = jasyptStringEncryptor.encrypt(plainText);
		String decryptedText = jasyptStringEncryptor.decrypt(encryptedText);
		assertThat(decryptedText).isEqualTo(plainText);
	}

	/**
	 * 운영에서 쓸 ENC 값을 뽑는 헬퍼.
	 * 출력된 문자열을 그대로 복사해 application.yml에 `ENC(...)`로 감싸 붙인다.
	 */
	@Test
	void print_enc_values() {
		String userEnc = jasyptStringEncryptor.encrypt("identity-service-1");
		String pwdEnc = jasyptStringEncryptor.encrypt("password123!");
		System.out.println("username = ENC(" + userEnc + ")");
		System.out.println("password = ENC(" + pwdEnc + ")");
	}
}
