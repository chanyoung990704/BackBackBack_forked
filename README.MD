# BackBackBack

Spring Boot 기반 백엔드 프로젝트입니다.  
게시판/인증/기업 데이터/리포트 기능을 제공하며, 배포는 AWS CodeBuild + CodeDeploy 기반으로 운영합니다.

## 프로젝트 주제 (What & Why)
BackBackBack은 **기업 재무/뉴스/리포트 데이터를 통합해 투자 판단을 돕는 분석형 백엔드**입니다.  
단순 게시판 서비스가 아니라, 기업 코드(종목코드) 기준으로 다음을 한 흐름으로 제공합니다.

- 기업 기본 정보/분기별 지표 조회
- AI 리포트 생성 및 PDF 연계 조회
- 워치리스트 기반 모니터링
- 인증/권한 기반 사용자 기능 분리(USER/ADMIN)

즉, 이 프로젝트의 핵심 목적은 **여러 데이터 소스(정형 재무 데이터 + 뉴스/AI 결과)를 API로 표준화해, 프론트엔드/운영 배치에서 재사용 가능한 분석 플랫폼을 만드는 것**입니다.

### 주요 사용자 시나리오
- 일반 사용자: 관심 기업을 워치리스트에 등록하고 최신 지표/인사이트를 조회
- 관리자: 기업 데이터 동기화, 리포트 적재/발행, 운영성 배치 관리
- 개발/운영자: CodeBuild/CodeDeploy 파이프라인으로 일관된 배포 및 롤백 수행

## 목차
- 프로젝트 개요
- 기술 스택
- 디렉터리 구조
- 빠른 시작 (로컬 JVM)
- 빠른 시작 (로컬 Docker)
- 환경변수 가이드
- 테스트 및 검증
- 배포 파이프라인
- 롤백 가이드
- 트러블슈팅
- 협업 규칙 및 문서
- ERD / DB 스키마

## 프로젝트 개요
- API 서버: Spring Boot 3.5.9, Java 21
- 저장소: MySQL, Redis
- 마이그레이션: Flyway
- 인증: JWT, Spring Security
- 운영 배포: CodeBuild -> ECR -> CodeDeploy -> EC2

## 기술 스택
- Language: Java 21
- Framework: Spring Boot 3.5.9
- Build: Gradle 8.14.3
- DB: MySQL 8, H2(test/perf)
- Cache: Redis 7
- Infra: AWS ECR, CodeBuild, CodeDeploy, EC2

## 디렉터리 구조
```text
.
├── src/
│   ├── main/resources/
│   │   ├── application*.yaml
│   │   └── db/migration/
│   └── test/
├── scripts/                       # CodeDeploy 훅 스크립트
│   ├── lib/deploy-runtime.sh      # 런타임 분기 공통 로직
│   └── tests/deploy-runtime-test.sh
├── docker-compose.app.yml         # 운영 기준 compose
├── docker-compose.local.yml       # 로컬 오버라이드 compose
├── buildspec.yml                  # CodeBuild
├── appspec.yml                    # CodeDeploy
└── README.MD
```

## 빠른 시작 (로컬 JVM)

### 1) 사전 준비
- Java 21
- 로컬 MySQL/Redis 실행
- `.env.example` 참고해 환경변수 주입

### 2) 실행
```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/bigprj?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
export SPRING_DATASOURCE_USERNAME=bigprj
export SPRING_DATASOURCE_PASSWORD=bigprj
export REDIS_HOST=localhost
export REDIS_PORT=6379
export LOG_PATH=./logs
export LOG_FILE=./logs/project.log

./gradlew bootRun
```

### 3) 확인
- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

## 빠른 시작 (로컬 Docker)

운영용 compose(`docker-compose.app.yml`)에 로컬 오버라이드(`docker-compose.local.yml`)를 합쳐 실행합니다.

### 1) 사전 준비
```bash
cp .env.example .env
mkdir -p secrets/jwt logs upload/files
```

JWT 키 파일을 `secrets/jwt/private.pem`, `secrets/jwt/public.pem` 경로에 준비하세요.

### 2) 이미지 빌드
```bash
./gradlew bootJar
docker build -t local-backbackback:dev .
```

### 3) 실행
```bash
APP_IMAGE=local-backbackback:dev \
APP_ENV_FILE=.env \
LOCAL_ENV_FILE=.env \
docker compose -f docker-compose.app.yml -f docker-compose.local.yml up -d
```

### 4) 종료
```bash
APP_ENV_FILE=.env \
LOCAL_ENV_FILE=.env \
docker compose -f docker-compose.app.yml -f docker-compose.local.yml down
```

## 환경변수 가이드

### 필수 (공통)
- `SPRING_PROFILES_ACTIVE` (`dev` 또는 `prod`)
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_ISSUER`
- `JWT_PRIVATE_KEY_PATH`
- `JWT_PUBLIC_KEY_PATH`
- `JWT_CURRENT_KID`
- `APP_TOKEN_HASH_PEPPER_B64` (Base64 인코딩된 pepper, 토큰 해시 필수값)
- `APP_BASE_URL`
- `API_BASE_URL`

### 권장
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `SPRING_THREADS_VIRTUAL_ENABLED` (JDK virtual thread 토글)
- `APP_VIRTUAL_THREAD_ENABLED` (앱 비동기 실행기 전체 virtual thread 토글)
- `APP_VIRTUAL_THREAD_INSIGHT_ENABLED` (insightExecutor만 virtual thread 토글)
- `APP_VIRTUAL_THREAD_EMAIL_ENABLED` (emailExecutor만 virtual thread 토글)
- `APP_AI_JOB_KAFKA_ENABLED` (Kafka 기반 AI job 경로 토글)
- `TURNSTILE_SECRET_KEY`, `TURNSTILE_SITE_KEY`
- `EMAIL_USERNAME`, `EMAIL_PASSWORD`
- `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_S3_PREFIX`
- `LOG_PATH`, `LOG_FILE`

### Docker 배포 관련
- `DEPLOY_RUNTIME`: `systemd` 또는 `docker`
- `COMPOSE_FILE_PATH`: 기본 `/opt/project/docker-compose.app.yml`
- `IMAGE_ENV_FILE`: 기본 `/opt/project/image-uri.env`
- `APP_ENV_FILE`: compose가 읽을 env 파일 경로
- `REDIS_HOST_DOCKER`: docker 네트워크 redis 서비스명(권장 `redis`)
- `SPRING_THREADS_VIRTUAL_ENABLED`: 기본 `false`
- `APP_VIRTUAL_THREAD_ENABLED`: 기본 `false` (운영에서 비Kafka 비동기를 virtual thread로 쓰려면 `true` 권장)
- `APP_VIRTUAL_THREAD_INSIGHT_ENABLED`: 기본 `false`
- `APP_VIRTUAL_THREAD_EMAIL_ENABLED`: 기본 `false`
- `APP_AI_JOB_KAFKA_ENABLED`: 기본 `true`

### 샘플 파일
- 로컬 템플릿: `.env.example`
- 서버 템플릿: `scripts/systemd/backbackback.env.example`

## 테스트 및 검증

### 기본 테스트
```bash
./gradlew test
```

### 배포 스크립트 검증
```bash
bash -n scripts/*.sh scripts/lib/*.sh scripts/tests/*.sh
bash scripts/tests/deploy-runtime-test.sh
```

### 배포 후 헬스체크
- `scripts/health-check.sh`가 `/actuator/health` 200을 반환해야 합니다.

## 배포 파이프라인

현재 배포 경로:
1. CodeBuild: 테스트 -> Docker build -> ECR push
2. 아티팩트 생성: `image-uri.env`, `docker-compose.app.yml`, `scripts/**/*`, `appspec.yml`
3. CodeDeploy: 훅 실행으로 서버 기동/검증

주요 파일:
- `buildspec.yml`
- `appspec.yml`
- `scripts/setup-server.sh`
- `scripts/install-app.sh`
- `scripts/start-server.sh`
- `scripts/stop-server.sh`
- `scripts/health-check.sh`

ECR:
- `160885260227.dkr.ecr.ap-northeast-2.amazonaws.com/aivle-back-app`

## 롤백 가이드

### 1) 런타임 롤백
- `DEPLOY_RUNTIME=systemd`로 전환하면 기존 systemd(JAR) 경로로 복귀할 수 있습니다.

### 2) 이미지 롤백
- 이전 정상 태그로 `APP_IMAGE`를 지정해 재배포합니다.
- 배포 후 `scripts/health-check.sh`로 성공 여부를 확인합니다.

## 트러블슈팅

### 1) `Unable to locate credentials`
- 원인: EC2 IAM Role 미부착 또는 권한 부족
- 해결: Instance Profile 부착 및 ECR/CodeDeploy 권한 점검

### 2) `AccessDeniedException ... ecr:CreateRepository`
- 원인: 해당 Role에 `ecr:CreateRepository` 없음
- 해결: 콘솔에서 ECR 선생성 또는 권한 추가

### 3) `env file ... not found`
- 원인: `APP_ENV_FILE` 또는 `LOCAL_ENV_FILE` 경로 불일치
- 해결: 실행 시 env 파일 경로를 명시

### 4) JWT 키 로딩 실패
- 원인: `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`와 실제 마운트 경로 불일치
- 해결: 컨테이너 내부 경로(`/secrets/jwt/*.pem`)와 env 일치

### 5) Redis 접속 실패
- 원인: Docker 환경에서 `REDIS_HOST=localhost` 사용
- 해결: Docker에서는 `REDIS_HOST_DOCKER=redis` 사용

## 협업 규칙 및 문서

### AGENTS.md
- Codex 작업 규칙의 기준 문서입니다.
- 언어/브랜치/커밋/컨텍스트 규칙을 따릅니다.

### .github/PULL_REQUEST_TEMPLATE.md
- PR 작성 기본 템플릿입니다.

### .context.md
- 작업 컨텍스트 기록 파일입니다.

## ERD / DB 스키마
- ERD 요약: `ERD.md`
- Flyway DDL 원본: `src/main/resources/db/migration/V1__init.sql`
