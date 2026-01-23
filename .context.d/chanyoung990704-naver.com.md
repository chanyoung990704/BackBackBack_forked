# Per-User Session Log

## 1. Owner (소유자)
- id: chanyoung990704-naver.com
- source: git config user.email
- email: chanyoung990704@naver.com

## 2. Recent Notes (최근 메모)
- 2026-01-23 | 작업: 서비스 시그니처를 UserEntity/ID 기반으로 정리 | 결과: Post/Comment 서비스 중복 사용자 조회 제거 및 테스트 수정 | 이슈: 없음
- 2026-01-23 | 작업: @CurrentUser 리졸버 추가 및 컨트롤러 적용 | 결과: Post/Comment 컨트롤러에서 JWT subject 파싱 제거, 관련 테스트 통과 | 이슈: 없음
- 2026-01-23 | 작업: prod 이메일 인증 설정 추가 | 결과: application-prod.yaml에 base-url 및 skip 설정 추가 | 이슈: 없음
- 2026-01-23 | 작업: 이메일 인증 URL 외부 접속 대응 | 결과: 인증 링크 base-url 설정 추가 및 EmailService에서 구성값 사용 | 이슈: 없음
- 2026-01-23 | 작업: 미인증 로그인 응답 분리 및 이메일 발송 테스트 보강 | 결과: AuthErrorCode/로그인 예외 처리 추가, 회원가입 이메일 발송 테스트 추가 | 이슈: 없음
- 2026-01-23 | 작업: compose 오버라이드 표준화 | 결과: docker-compose.override.yml 추가 및 dev 파일 제거 | 이슈: 없음
- 2026-01-23 | 작업: docker compose dev/prod 오버라이드 추가 | 결과: docker-compose.dev.yml/docker-compose.prod.yml 추가 및 기본 compose 정리 | 이슈: 없음
- 2026-01-23 | 작업: Redis compose 파일 통합 | 결과: docker-compose.redis.yml 삭제 후 docker-compose.yml로 통합 | 이슈: 없음
- 2026-01-23 | 작업: Redis docker compose 파일 추가 | 결과: 개발용 Redis 실행용 docker-compose.redis.yml 추가 | 이슈: 없음
- 2026-01-23 | 작업: dev 회원가입 이메일 인증 스킵 처리 | 결과: dev 프로파일에서 이메일 인증 생략 및 SignUpService 테스트 보강 | 이슈: 없음
- 2026-01-23 | 작업: 이메일 인증 기능 추가 | 결과: 이메일 인증 엔티티/서비스/컨트롤러, 마이그레이션 및 테스트 추가 | 이슈: 없음
- 2026-01-23 | 작업: 테스트 메일 빈 모킹 및 dev 이메일 설정 import | 결과: 테스트용 JavaMailSender 모킹 추가, dev 프로파일에서 application-email.yml 로딩 | 이슈: 없음
- 2026-01-22 | 작업: 댓글 CRUD 컨트롤러 및 통합 테스트 추가 | 결과: CommentController와 CRUD 통합 테스트 추가, 전체 테스트 통과 | 이슈: 없음
- 2026-01-22 | 작업: Repository 슬라이스 테스트 추가 | 결과: PostsRepository/CommentsRepository DataJpaTest 추가 및 전체 테스트 통과 | 이슈: 없음
- 2026-01-22 | 작업: AccessTokenBlacklistService 단위 테스트 추가 | 결과: 블랙리스트/로그아웃 기준 로직 단위 테스트와 전체 테스트 통과 | 이슈: 없음
- 2026-01-22 | 작업: 서비스 레이어 단위 테스트 추가 | 결과: PostService/UserDomainService 테스트 추가 및 전체 테스트 통과 | 이슈: 없음
- 2026-01-22 | 작업: 게시글 JWT 컨트롤러/페이징 및 테스트 보강 | 결과: /posts 페이징 API 및 JWT subject 검증 테스트 추가, CommonException 핸들러 추가 | 이슈: 없음
- 2026-01-21 | 작업: RefreshTokenServiceTest 불필요 스텁 제거 | 결과: 테스트별로 Redis 스텁 주입하도록 변경 | 이슈: 없음
- 2026-01-21 | 작업: 회원가입 API 추가 | 결과: /auth/signup, USER 역할 매핑, 서비스/DTO/테스트 추가 | 이슈: 없음
- 2026-01-21 | 작업: 로그아웃 전체 로그아웃 기준 시각 검증 보정 | 결과: issuedAt == logoutAllAt 허용 및 검증 테스트 추가 | 이슈: 없음
- 2026-01-21 | 작업: 로그아웃/전체 로그아웃 및 블랙리스트 구현 | 결과: Access 토큰 블랙리스트/전체 로그아웃 검증 추가, 로그아웃 API 및 리프레시 토큰 폐기 로직/테스트 보강 | 이슈: 없음
- 2026-01-21 | 작업: 업스트림 로그아웃 이슈 등록 | 결과: 로그아웃/전체 로그아웃 및 Access Token 블랙리스트 이슈 생성 | 이슈: 없음
- 2026-01-21 | 작업: 테스트 보안 설정 정리 | 결과: TestSecurityConfig 추가 및 테스트 전반 Import 적용, JWT 키 파일 의존 제거 | 이슈: 없음
- 2026-01-21 | 작업: 인증 통합 테스트/의존성 정리 | 결과: Auth 통합 테스트와 Redis 컨테이너 적용, 테스트 의존성 최소화, 실패 케이스 단위 테스트 보강 | 이슈: 없음
- 2026-01-21 | 작업: 전역 예외 처리 리팩터링 | 결과: 공통 에러 코드/필드 에러 응답/GlobalExceptionHandler 추가 및 Auth 핸들러 통합 | 이슈: 없음
- 2026-01-20 | 작업: 이슈 템플릿 추가 | 결과: 일반 이슈 템플릿 생성 | 이슈: 없음
- 2026-01-20 | 작업: 테스트 설명/주석 보강 및 오류 수정 | 결과: Auth/RefreshToken 테스트 DisplayName과 given-when-then 추가 | 이슈: 없음
- 2026-01-20 | 작업: JWT 인증 단위 테스트 추가 | 결과: Auth/RefreshToken/JwtToken 테스트 작성 | 이슈: 없음
- 2026-01-20 | 작업: 레거시 ROLE_ 토큰 컷오프 적용 | 결과: 컷오프 이후 인증 실패 처리 | 이슈: 없음
- 2026-01-20 | 작업: JWT 역할 호환 전환 구현 | 결과: ROLE_ 레거시 허용/만료 설정 추가 | 이슈: 없음
- 2026-01-20 | 작업: JWT 권한 변환기 수정 | 결과: roles 클레임 처리 방식 권장안 반영 | 이슈: 없음
- 2026-01-20 | 작업: JWT 인증 후속 작업 계획 정리 | 결과: 로그아웃/블랙리스트/Lua 동시성/마이그레이션 단계화 | 이슈: 없음
- 2026-01-20 | 작업: JWT 인증 Phase1 구성 | 결과: 설정/토큰/리프레시 저장소 및 로그인·리프레시 API 추가 | 이슈: 없음
- 2026-01-20 | 작업: JWT 인증 로직 추가 준비 | 결과: feat/jwt-auth 브랜치 생성 | 이슈: 없음
- 2026-01-20 | 작업: withdraw 재호출 동작 테스트 추가 | 결과: 삭제 상태 유지 검증 | 이슈: 없음
- 2026-01-20 | 작업: UserEntity UUID 자동 생성 테스트 추가 | 결과: 저장 시 UUID 생성 검증 | 이슈: 없음
- 2026-01-20 | 작업: UserDetails 테스트 DisplayName 추가 | 결과: 테스트 목적을 한국어로 명시 | 이슈: 없음
- 2026-01-20 | 작업: GrantedAuthority 제네릭 타입 오류 수정 | 결과: CustomUserDetails 스트림 타입 명시 | 이슈: 없음
- 2026-01-20 | 작업: 코드 주석 한글화 | 결과: main/test 주석을 한국어로 변경 | 이슈: 없음
- 2026-01-20 | 작업: 코드 전반 간단 주석 추가 | 결과: main/test 클래스에 1~2줄 설명 주석 삽입 | 이슈: 없음
- 2026-01-20 | 작업: UserEntity 테스트 생성자 접근 이슈 해결 | 결과: 리플렉션 기반 생성 로직 추가 | 이슈: 없음
- 2026-01-20 | 작업: UserDetails/withdraw 테스트 추가 | 결과: H2 기반 서비스/도메인 테스트 작성 | 이슈: 없음
- 2026-01-20 | 작업: UserEntity 삭제/탈퇴 도메인 메서드 추가 | 결과: withdraw로 상태 전이와 soft delete 처리 | 이슈: 없음
- 2026-01-20 | 작업: UserDetails 조회 fetch join 최적화 | 결과: 이메일 기준 user_roles fetch join 추가 | 이슈: 없음
- 2026-01-20 | 작업: 분리형 UserDetails 구현 | 결과: CustomUserDetails/Service 및 저장소 추가 | 이슈: 없음
- 2026-01-20 | 작업: RoleName 값 수정 및 UserRoleEntity 추가 | 결과: USER/ADMIN 역할과 user_roles 매핑 엔티티 설계 | 이슈: 없음
- 2026-01-20 | 작업: UserEntity 정리 및 RoleEntity 추가 | 결과: roles 매핑 설계 및 BaseEntity 삭제 로직 개선 | 이슈: 없음
- 2026-01-20 | 작업: UserEntity/Status 추가 | 결과: users 테이블 매핑 엔티티 설계 | 이슈: 없음
- 2026-01-20 | 작업: H2 콘솔 접근 문제 완화 설정 추가 | 결과: dev 프로필 보안 설정에 H2 콘솔 허용 | 이슈: 없음
- 2026-01-20 | 작업: 개인 로그 파일 생성 | 결과: 템플릿 생성 | 이슈: 없음

## 3. History (이전 기록)
- YYYY-MM-DD | 작업: ... | 결과: ... | 이슈: ...
