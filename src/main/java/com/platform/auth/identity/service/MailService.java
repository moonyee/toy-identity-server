package com.platform.auth.identity.service;

/**
 * 메일 발송 추상 계약. 프로바이더 교체(Naver/Gmail/SES 등)를 쉽게 하기 위해
 * 인터페이스로 분리한다.
 *
 * <p>현재 구현체: {@link NaverMailService} (Naver SMTP 465 SSL).
 *
 * <p>호출 규약:
 * <ul>
 *   <li>구현체는 반드시 비동기({@code @Async("mailExecutor")})로 실행되어 호출자를 막지 않는다.</li>
 *   <li>발송 실패 시 예외를 전파하지 않고 {@link MailSendFailureHandler}에 위임한다.</li>
 *   <li>인증 코드 원문을 로그에 남기지 않는다(prefix 2자리 + {@code ****} 만 허용).</li>
 * </ul>
 */
public interface MailService {

    /**
     * 이메일 인증 코드를 포함한 검증 메일을 발송한다.
     *
     * @param email 수신자 이메일 주소
     * @param code 6자리 인증 코드 (Redis {@code auth:code:{email}} 에 저장된 값과 동일)
     */
    void sendVerificationMail(String email, String code);
}
