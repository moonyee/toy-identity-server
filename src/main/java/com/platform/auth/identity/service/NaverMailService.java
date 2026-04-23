package com.platform.auth.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Naver SMTP(smtp.naver.com:465, implicit SSL) 기반 메일 발송 구현.
 *
 * <p>{@code @Async("mailExecutor")}로 가상 스레드에서 실행되므로 호출자 스레드를
 * 블록하지 않는다. 발송 실패 시 예외를 전파하지 않고
 * {@link MailSendFailureHandler}에 위임한다 — 회원가입 트랜잭션 커밋 이후 실행되므로
 * 실패가 가입 자체를 롤백시켜선 안 된다.
 *
 * <p>주의:
 * <ul>
 *   <li>Naver 계정이 2단계 인증일 경우 반드시 앱 비밀번호를 발급해서 {@code NAVER_MAIL_PASSWORD}로 주입.</li>
 *   <li>port 465는 implicit SSL이므로 {@code mail.smtp.ssl.enable=true} + SSLSocketFactory 필수.
 *       STARTTLS 활성화는 충돌 원인이 되므로 꺼둔다.</li>
 *   <li>인증 코드 원문은 로그에 남기지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MailSendFailureHandler failureHandler;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 기동 시 SMTP 자격 증명 상태를 확인한다. 비어 있으면 메일 발송이 반드시 실패하므로
     * 조기에 WARN 로그로 경고해 환경변수 주입 누락을 빠르게 발견할 수 있게 한다.
     */
    @PostConstruct
    void warnIfCredentialsMissing() {
        if (from == null || from.isBlank()) {
            log.warn("### spring.mail.username (NAVER_MAIL_USERNAME) is blank — verification mails will fail.");
        }
        if (mailSender instanceof JavaMailSenderImpl impl) {
            if (impl.getPassword() == null || impl.getPassword().isBlank()) {
                log.warn("### spring.mail.password (NAVER_MAIL_PASSWORD) is blank — SMTP AUTH will fail (535).");
            }
        }
    }

    @Async("mailExecutor")
    @Override
    public void sendVerificationMail(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("[Identity] 이메일 인증 코드");
            helper.setText(buildHtml(code), true);

            mailSender.send(message);
            log.info("### Verification mail sent to={}, codePrefix={}****",
                maskEmail(email), code.substring(0, Math.min(2, code.length())));
        } catch (Exception e) {
            // SLF4J는 마지막 Throwable 인자를 스택트레이스로 처리한다 (포맷 placeholder 개수 유지).
            log.warn("### Verification mail send failed to={}, err={}",
                maskEmail(email), e.getMessage(), e);
            failureHandler.handle(email, code, e);
        }
    }

    private String buildHtml(String code) {
        return "<!doctype html><html><body style=\"font-family:sans-serif\">"
             + "<h2>이메일 인증 코드</h2>"
             + "<p>아래 인증 코드를 입력해 주세요. 유효 시간은 <strong>3분</strong>입니다.</p>"
             + "<div style=\"font-size:28px;font-weight:bold;letter-spacing:6px;padding:16px 24px;"
             + "background:#f4f4f4;display:inline-block;border-radius:8px;\">" + code + "</div>"
             + "<p style=\"color:#888;font-size:12px;margin-top:24px;\">"
             + "본 메일에 회신하지 마세요. 본인이 요청하지 않았다면 무시하시면 됩니다.</p>"
             + "</body></html>";
    }

    // 이메일 로그 노출 최소화: 앞 2자 + @ + 도메인.
    private String maskEmail(String email) {
        if (email == null) return "unknown";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String localMasked = local.length() <= 2 ? local.charAt(0) + "*"
            : local.substring(0, 2) + "*".repeat(Math.max(1, local.length() - 2));
        return localMasked + domain;
    }
}
