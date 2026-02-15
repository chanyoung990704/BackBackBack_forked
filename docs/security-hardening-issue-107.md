# 보안 하드닝 이행 기록 (#107)

## 1. 개요
- 대상: 인증 재전송(3), 토큰 해시 저장(4), refresh/logout CSRF 보호(6)
- 적용 일자: 2026-02-14

## 2. 변경 사항
### 2.1 인증 재전송 API
- 정식 경로를 `POST /api/auth/resend-verification`로 통일했습니다.
- `GET /api/auth/resend-verification`는 레거시 유예 경로로 유지하고 `Deprecation/Sunset/Warning` 헤더를 추가했습니다.
- 레거시 경로 Sunset 날짜를 `2026-02-28`(UTC: `Sat, 28 Feb 2026 00:00:00 GMT`)로 고정했습니다.
- 사용자/IP 기준 최소 레이트리밋을 Redis 기반으로 적용했습니다.

### 2.2 토큰 저장 구조
- refresh token:
  - DB: 평문 `token_value` 저장을 중단하고 `token_hash(SHA-256)` 저장으로 전환했습니다.
  - Redis: 키/세션 식별자를 토큰 해시 기반으로 전환했습니다.
  - dual-read: 해시 우선 조회, 레거시 평문 조회 fallback 지원.
- email verification token:
  - DB: 평문 `token` 저장을 중단하고 `token_hash(SHA-256)` 저장으로 전환했습니다.
  - 검증 시 해시 우선 조회 + 레거시 fallback을 지원합니다.
- Flyway:
  - MySQL/H2 모두 `V27__hash_auth_tokens.sql` 마이그레이션을 추가했습니다.

### 2.3 CSRF(더블서브밋) 보호
- 로그인/재발급 시 `csrf_token` 쿠키를 발급합니다.
- `POST /api/auth/refresh`, `POST /api/auth/logout`에서:
  - 쿠키 `csrf_token`
  - 헤더 `X-CSRF-Token`
  - 두 값의 일치 여부를 검증합니다.
- 불일치/누락 시 403(`AUTH_403`)으로 차단합니다.
- CORS 허용 헤더에 `X-CSRF-Token`을 추가했습니다.

### 2.4 QnA/댓글 접근 제어 보강
- `CommentController`: 댓글 목록 조회 시 `UserEntity`(@CurrentUser)를 주입받아 서비스로 전달하도록 수정했습니다.
- `CommentsService`: `listByPost`에서 `PostReadAccessPolicy`를 호출하여 QnA 게시글의 경우 작성자 본인 여부를 검증하도록 로직을 추가했습니다.
- 테스트: `CommentControllerIntegrationTest`의 접근 제어 실패 케이스를 해결했습니다.

## 3. 검증
- 수행 테스트
  - `EmailVerificationControllerTest`
  - `EmailVerificationServiceTest`
  - `RefreshTokenServiceTest`
  - `AuthIntegrationTest`
- 결과: 모두 통과

## 4. 후속 작업
- 다음 스프린트 시작 시점(2026-02-28 이후) `GET /api/auth/resend-verification` 경로를 물리 제거합니다.
