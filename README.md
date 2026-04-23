# identity-server

사용자 인증 도메인 서비스. 회원가입·이메일 인증·로그인·로그아웃·동시 세션 관리·회원 탈퇴·JWT 발급을 담당합니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 21 (Virtual Threads) |
| 프레임워크 | Spring Boot 3.5.9, Spring MVC |
| 포트 / 컨텍스트 | `8182` / `/api` |
| 데이터 | MariaDB (JPA/Hibernate), Redis |
| 인증 | Spring Security, JJWT 0.11.5 (HS256), DelegatingPasswordEncoder (BCrypt 기본) |
| 메일 | Naver SMTP (465 SSL) via JavaMailSender |
| 암호화 | Jasypt (`PBEWithHmacSHA512AndAES_256`, iter 310,000) |
| 비동기 | `@Async` + Virtual Thread Executor |
| 문서화 | springdoc-openapi 2.7.0 (OpenAPI 3) |
| 빌드 | Gradle |

## 아키텍처 위치

```
Client ─▶ Gateway(:8180) ─▶ identity-server(:8182)
                         ─▶ edge-server(:8181)
```

Gateway는 JWT 검증·라우팅·CircuitBreaker를 담당하고, identity는 사용자 도메인 로직에 집중합니다. Gateway 통과 후에도 identity 자체 `JwtAuthenticationFilter`가 이중 검증을 수행합니다(Gateway 우회 방어).

## API

모든 엔드포인트는 context-path `/api` 가 prefix 됩니다.

### 공개 (permit)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/login` | 로그인, JWT 발급 |
| POST | `/api/auth/join` | 회원가입(PENDING) + 인증 메일 발송 |
| GET | `/api/auth/check-id?userId=...` | 아이디 중복 검사 |
| GET | `/api/auth/check-email?email=...` | 이메일 중복 검사 |
| POST | `/api/v1/auth/verify` | 6자리 코드 검증 → ACTIVE 전이 |
| POST | `/api/v1/auth/resend` | 인증 코드 재발송 |

### 인증 필요 (Bearer)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/auth/logout` | 현재 토큰의 Redis 세션 삭제 |
| DELETE | `/api/v1/auth/me` | 회원 탈퇴 (Soft delete, 비밀번호 재확인) |

### OpenAPI / Swagger
- Swagger UI: <http://localhost:8182/api/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8182/api/v3/api-docs>

## Redis 키 계약

| 키 | 값 | TTL | 용도 |
|----|-----|-----|------|
| `AUTH:{userId}:{jti}` | jti | 3600s | 활성 세션 존재 증명 |
| `USER_SESSIONS:{userId}` | Set(jti...) | 3600s | 동시 세션 카운팅 |
| `auth:code:{email}` | 6자리 코드 | 180s | 이메일 인증 코드 |

Gateway의 JWT 검증 필터가 `AUTH:*` 키 존재를 확인하므로 로그아웃·탈퇴 시 즉시 401로 떨어집니다.

## 주요 설계

- **JWT**: HS256, 1시간 TTL, 클레임 `sub=userId / jti=UUID / role`. 비밀키는 `JWT_SECRET` 환경변수를 Gateway와 공유.
- **동시 세션 한도**: 기본 3 (`auth.max-session`). Lua 스크립트로 SCARD + SADD를 원자화하여 동시 로그인 경쟁 방지.
- **비밀번호 해싱**: `DelegatingPasswordEncoder`, `idForEncode="bcrypt"` (strength 12). Argon2 전환 시 `idForEncode`만 변경하면 기존 `{bcrypt}` 해시는 자동 매칭.
- **사용자 존재 은폐**: 로그인 실패 시 "아이디 없음"과 "비밀번호 틀림" 모두 `USER_NOT_FOUND`. 탈퇴(`DELETED`) 계정도 동일 응답으로 은폐.
- **이메일 인증**: 회원가입 시 PENDING → 6자리 코드 발송 → 3분 내 `/v1/auth/verify`로 ACTIVE 전이. 로그인은 ACTIVE만 허용.
- **메일 발송**: `UserRegisteredEvent` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("mailExecutor")` (Virtual Thread). 트랜잭션 롤백 시 메일 미발송 보장.
- **탈퇴**: Soft delete (`UserStatus.DELETED` + `withdrawnAt`) + `UserWithdrawnEvent` AFTER_COMMIT 리스너가 Redis 세션·코드 키 일괄 삭제. 비밀번호 재확인 필수.
- **에러 응답**: 모두 `ApiResponseEntity<T>` 래퍼. `GlobalExceptionHandler`가 `ErrorCode` enum 기반으로 자동 응답.

## 환경변수 (기동 전 주입 필수)

| 변수 | 설명 |
|------|------|
| `JWT_SECRET` | JWT 서명 비밀키. **Gateway와 동일 값 필수** |
| `JASYPT_PASSWORD` | DB 접속정보(`ENC(...)`) 복호화 마스터 비밀번호 |
| `NAVER_MAIL_USERNAME` | 메일 발송 계정 풀 이메일 (예: `alice@naver.com`) |
| `NAVER_MAIL_PASSWORD` | Naver 앱 비밀번호. **2단계 인증 계정은 반드시 앱 비밀번호 사용** |

Naver 메일 설정 사전 확인:
1. 네이버메일 > 환경설정 > POP3/IMAP 설정에서 **IMAP/SMTP 사용** + **IMAP/SMTP 기기 사용 설정 ON**
2. 2FA 계정은 별도 앱 비밀번호 발급

## 실행

### 사전 요구사항
- JDK 21
- Docker (MariaDB, Redis)

### 의존 인프라 기동
```bash
docker compose up -d     # compose.yaml 기준 MariaDB 3306 + Redis master(6379)/slave(6380)
```

### 애플리케이션 실행
```bash
# PowerShell 예시
$env:JWT_SECRET="please-override-in-real-env-with-at-least-32-bytes"
$env:JASYPT_PASSWORD="..."
$env:NAVER_MAIL_USERNAME="alice@naver.com"
$env:NAVER_MAIL_PASSWORD="앱비밀번호"

./gradlew bootRun
```

### 테스트 / 빌드
```bash
./gradlew test
./gradlew build
```

## 디렉토리 구조

```
src/main/java/com/platform/auth/identity/
├── IdentityServiceApplication.java
├── common/
│   ├── config/        # AsyncConfig, JasyptConfig, JwtTokenUtil, OpenApiConfig, SecurityConfig
│   ├── filter/        # JwtAuthenticationFilter (Servlet)
│   └── response/      # ApiResponseEntity
├── controller/        # LoginController, JoinController, VerifyController, WithdrawController
│   └── dto/           # LoginDto, JoinDto, VerifyDto, WithdrawDto
├── domain/
│   ├── entity/        # User, UserStatus, UserRole
│   └── repository/    # UserRepository
├── event/             # UserRegisteredEvent(+Listener), UserWithdrawnEvent(+Listener)
├── exception/         # ErrorCode, ErrorException, GlobalExceptionHandler
└── service/           # JoinService, LoginService, VerifyService, WithdrawService,
                       # MailService(+NaverMailService), MailSendFailureHandler(+LogOnly)
```

## 알려진 제약 및 후속 과제

- **HS256 → JWKS 전환** 권장 (현재는 Gateway와 비밀키 공유)
- **Hard-delete 배치** 미구현 — `withdrawnAt` 경과 후 row 삭제 + 개인정보 익명화
- **Rate limit 없음** — `/v1/auth/resend`, 로그인 실패 횟수 등에 token bucket 도입 필요
- **메일 DLQ 미구현** — 현재는 `LogOnlyMailFailureHandler`만. Redis Stream 기반 재시도 워커로 교체 예정
- **ENC() 값 교체** — 운영 `application.yml`의 `spring.datasource.username/password`는 운영 Jasypt 키로 재암호화한 값으로 교체 필요
