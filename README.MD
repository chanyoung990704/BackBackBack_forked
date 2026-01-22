# Repository Guidelines

이 문서는 작업 시 참고해야 할 운영 규칙과 파일별 가이드라인을 제공합니다.

## 파일별 가이드라인

### AGENTS.md
- Codex(AI Agent) 작업 규칙의 단일 기준 문서입니다.
- 변경이 필요한 경우에만 최소 범위로 수정합니다.
- 문서의 언어 규칙(한국어)은 항상 유지합니다.

### .github/PULL_REQUEST_TEMPLATE.md
- PR 작성 시 기본 템플릿으로 사용합니다.
- 섹션을 삭제하지 말고, 변경 사항과 테스트 결과를 간결하게 작성합니다.

### .context.md
- 작업 컨텍스트를 기록하는 파일이며, 루트 경로에 둡니다.
- 작업 시작 전 읽고, 작업 종료 후 반드시 업데이트합니다.
- 형식은 `AGENTS.md`의 컨텍스트 템플릿을 따릅니다.

## 로컬 실행 예시 (ENV 설정)
- `.env.example`를 참고해 필요한 환경 변수를 주입합니다.
- Spring Boot는 기본적으로 `.env`를 자동 로딩하지 않으므로, 셸/IDE 실행 설정에서 환경 변수를 지정합니다.

```bash
export SPRING_PROFILES_ACTIVE=dev
export DEV_DB_URL=jdbc:h2:mem:devdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
export DEV_DB_USERNAME=sa
export DEV_DB_PASSWORD=
export H2_CONSOLE_ENABLED=true
export H2_CONSOLE_PATH=/h2-console

./gradlew bootRun
```
