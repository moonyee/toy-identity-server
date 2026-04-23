package com.platform.auth.identity.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.platform.auth.identity.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원가입 이벤트 리스너.
 *
 * <p>{@code @TransactionalEventListener(AFTER_COMMIT)} 로 DB 커밋 후에만 실행되므로
 * 트랜잭션이 롤백되면 메일은 발송되지 않는다.
 *
 * <p>{@code @Async("mailExecutor")}로 가상 스레드 위에서 처리되어 회원가입 응답을 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventListener {

    private final MailService mailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        mailService.sendVerificationMail(event.getEmail(), event.getCode());
    }
}
