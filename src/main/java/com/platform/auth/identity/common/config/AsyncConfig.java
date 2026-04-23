package com.platform.auth.identity.common.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @Async 전용 실행기. Java 21 가상 스레드를 사용해 메일 발송 같은 외부 I/O 작업이
 * 플랫폼 스레드를 점유하지 않도록 한다.
 *
 * <p>주의: 가상 스레드는 I/O 중심 작업에 최적. CPU-bound 작업에는 별도 고정 풀을 쓰는 것이
 * 안전하며, synchronized 블록·native 라이브러리는 pin 이슈를 유발할 수 있다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 메일 발송 등 외부 I/O를 가상 스레드로 실행한다.
     * {@code @Async("mailExecutor")}로 연결해 사용한다.
     */
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
