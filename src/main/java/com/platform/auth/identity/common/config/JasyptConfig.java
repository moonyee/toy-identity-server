package com.platform.auth.identity.common.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

	/**
	 * Jasypt 마스터 비밀번호.
	 * 절대 application.yml에 평문으로 두지 말고, 운영에서는 환경변수
	 * `JASYPT_PASSWORD` 또는 외부 Vault에서 주입한다.
	 */
	@Value("${jasypt.encryptor.password}")
	private String password;

	@Bean("jasyptStringEncryptor")
	public StringEncryptor stringEncryptor() {
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();

		config.setPassword(password);
		// 구시대 PBEWithMD5AndDES 대신 AES-256 + HMAC-SHA512.
		// 주의: JDK 17 표준 알고리즘 이름은 'PBEWithHmacSHA512AndAES_256' (Hmac 소문자)이며,
		// 대소문자가 달라지면 NoSuchAlgorithmException이 발생하니 바꾸지 말 것.
		config.setAlgorithm("PBEWithHmacSHA512AndAES_256");
		config.setKeyObtentionIterations("310000"); // OWASP 2023 권장치 이상
		config.setPoolSize("1");
		config.setProviderName("SunJCE");
		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
		// AES-256 모드에서는 IV가 필수. RandomIv로 평문→암호문 매핑을 깨뜨려야 한다.
		config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
		config.setStringOutputType("base64");

		encryptor.setConfig(config);
		return encryptor;
	}
}
