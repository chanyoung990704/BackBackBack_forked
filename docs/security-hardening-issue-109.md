# 보안 하드닝 이행 기록 (#109)

## 1. 개요
- 대상: 토큰 해시 pepper 적용 및 설정 Fail-Fast
- 적용 일자: 2026-02-14

## 2. 변경 사항
### 2.1 TokenHashService 강화
- 기존 `SHA-256(token)`에서 `HMAC-SHA256(token, pepper)`로 전환했습니다.
- pepper는 `app.security.token-hash.pepper-base64` 설정값을 사용합니다.
- 환경변수 `APP_TOKEN_HASH_PEPPER_B64`가 누락되거나 Base64 형식이 아니면 애플리케이션이 기동 실패하도록 처리했습니다.

### 2.2 레거시 해시 호환
- `RefreshTokenService`, `EmailVerificationService`에서 조회 시 신규 해시 우선, 레거시 SHA-256 해시 fallback을 지원합니다.
- 레거시 해시로 조회된 데이터는 성공 경로에서 신규 HMAC 해시로 마이그레이션합니다.

### 2.3 설정/문서 반영
- `.env.example`에 `APP_TOKEN_HASH_PEPPER_B64` 항목을 추가했습니다.
- `README.MD` 필수 환경변수 목록에 토큰 해시 pepper 설정을 반영했습니다.

## 3. 검증
- 수행 테스트
  - `./gradlew cleanTest test`
- 결과: 전체 통과

## 4. 운영 적용 가이드
- 서버 env 파일(`APP_ENV_FILE`)에 `APP_TOKEN_HASH_PEPPER_B64`를 설정합니다.
- 값은 **Base64 인코딩된 랜덤 바이트**여야 하며 코드/저장소에 하드코딩하지 않습니다.
