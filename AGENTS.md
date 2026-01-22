# **Codex Working Guidelines (AGENTS.MD)**

이 문서는 이 저장소에서 Codex(AI Agent)가 따라야 할 작업 규칙을 정의한다.  
이 지침은 \*\*절대적(Mandatory)\*\*이며, 모든 제안과 코드 생성 시 최우선으로 적용된다.

## **0\. Meta Rules (Language Protocol)**

가장 중요한 언어 규칙은 다음과 같다.

1. **Interaction (대화 및 문서):** 모든 설명, 질문, 가이드, PR 본문은 \*\*한국어(Korean)\*\*로 작성한다.
2. **Git System (버전 관리):** 브랜치 이름과 커밋 메시지는 반드시 \*\*영어(English)\*\*로 작성한다.
3. **Exception (영어 사용 예외):** 사용자가 명시적으로 영어 응답을 요청한 경우 **또는** 사용자 개인 로그에 영어 응답 선호가 명시된 경우 해당 요청과 응답은 영어로 작성한다.

## **1\. 기본 원칙 (Core Principles)**

* **Context Awareness:** 기존 설계, 아키텍처, 코딩 컨벤션을 최우선으로 존중한다.
* **Minimal Changes:** 불필요한 리팩토링이나 스타일 변경을 피하고, 요청된 범위만 수정한다.
* **Ask Before Assume:** 요구 사항이 모호하거나 여러 해석이 가능한 경우, 추측하여 코딩하지 말고 먼저 질문한다.
* **Immutable Files:** 자동 생성 파일(build/, dist/, .generated 등)은 직접 수정하지 않는다.

## **2\. 보안 및 안전 수칙 (Security & Safety)**

* **No Secrets:** API Key, Password, Token 등 민감 정보는 절대로 코드에 하드코딩하지 않는다. (환경 변수 사용)
* **Input Validation:** 사용자 입력값에 대한 검증 로직을 항상 고려한다.
* **Deprecation Warning:** 사용되지 않는(Deprecated) 라이브러리나 메서드 사용을 지양하고, 최신 안정 버전을 사용한다.

## **3\. 작업 흐름 (Workflow)**

1. **Load Context:** 작업 시작 전 .context.md와 개인 로그(.context.d/{collaboratorId}.md)를 읽어 현재 진행 상황을 파악한다. (See Section 10, 11)
2. **Analysis:** 변경 전 관련 파일을 읽고 전체 맥락과 의존성을 파악한다.
3. **Strategy:** 수정 범위와 영향 범위를 먼저 생각한 뒤 작업을 시작한다.
4. **Implementation:** 변경은 가능한 한 작은 단위(Atomic)로 수행한다.
5. **Verification:** 수정 후 컴파일 에러 가능성, 린트(Lint) 위반 여부를 스스로 점검한다.
6. **Suggestion:** 필요 시 간단한 테스트 방법 또는 검증 절차(cURL 예시 등)를 제안한다.
7. **Update Context:** 커밋 전에 개인 로그를 읽고, 공유할 가치가 있는 변경을 .context.md에 기록한다.

## **4\. 코드 스타일 (Code Style)**

* **Conventions:** 프로젝트에 .editorconfig, .prettierrc, checkstyle.xml 등이 있다면 이를 엄격히 준수한다.
* **Naming:**
    * Class: PascalCase
    * Method/Variable: camelCase
    * Constant: UPPER\_SNAKE\_CASE
    * DB Table/Column: 프로젝트 기존 규칙(주로 snake\_case)을 따른다.
* **Clean Code:** 중복 코드는 최소화하고(DRY), 함수는 하나의 기능만 수행하도록(SRP) 작성한다.

## **5\. 스프링 / 자바 규칙 (Spring & Java Specifics)**

* **Layered Architecture:**
    * Controller: 요청/응답 처리, 파라미터 검증 위주. 비즈니스 로직 포함 금지.
    * Service: 비즈니스 로직, 트랜잭션 관리(@Transactional).
    * Repository: DB 접근 로직만 담당.
* **API Compatibility:** 공개 API 변경 시 하위 호환성(Backward Compatibility) 유지 여부를 반드시 확인하고 설명한다.
* **Exception Handling:** try-catch로 에러를 삼키지 말 것. 적절한 Custom Exception을 던지거나 Global Exception Handler에 위임한다.

## **6\. 테스트 (Testing)**

* **Impact Analysis:** 변경 코드가 기존 테스트를 깨뜨리는지 확인한다.
* **Unit Test:** 새로운 비즈니스 로직 추가 시, 해당 로직에 대한 단위 테스트 코드를 제안한다.
* **Guide:** 테스트 실행 명령어(./gradlew test 등)를 함께 안내한다.
* **Given/When/Then:** 테스트 본문은 given/when/then 형식의 주석을 사용해 의도를 명확히 한다.

### **6.1 테스트 중 자주 발생한 문제 (회고/주의)**

* **OAuth2 JwsHeader import 혼동:** `org.springframework.security.oauth2.jose.jws.JwsHeader`가 아닌 `org.springframework.security.oauth2.jwt.JwsHeader`를 사용해야 한다.
* **Mockito 제네릭 캡처 오류:** `when(...).thenReturn(...)`가 제네릭 캡처에 실패하면 `doReturn(...).when(...)`로 우회한다.
* **UnnecessaryStubbingException:** 사용하지 않는 스텁이 있으면 테스트가 실패하므로 불필요한 스텁은 제거한다.
* **JWT iss 클레임 타입:** `claims.getIssuer()`는 URL 변환을 기대하므로 문자열 issuer는 `claims.getClaims().get("iss")`로 검증한다.

## **7\. 문서화 (Documentation)**

* **Sync Code & Docs:** 로직이 변경되면 주석(Javadoc/KDoc)과 관련 문서(README, API 명세)도 함께 갱신한다.
* **Tone:** 문서는 명확하고 정중한 어조(해요체 또는 건조체)를 유지한다.

## **7.1 코드 주석 원칙**

* **Korean Comments:** 코드 생성 시 이해를 돕는 최소한의 한글 주석을 포함한다. (과도한 주석 금지)

## **8\. Git Branch Naming Convention**

브랜치는 다음 형식을 따른다. 소문자와 하이픈(-)만 사용한다.  
{type}/{description}

### **Branch Types**

* feat/ : 새로운 기능 개발 (New features)
* fix/ : 버그 수정 (Bug fixes)
* hotfix/ : 운영 환경 긴급 수정 (Critical fixes for prod)
* refactor/ : 기능 변경 없는 코드 개선 (Code restructuring)
* docs/ : 문서 작업 (Documentation)
* test/ : 테스트 코드 작업 (Adding tests)
* chore/ : 설정, 의존성, 빌드 작업 (Build, configs)

### **Examples**

* feat/oauth-login
* fix/payment-retry-logic
* refactor/user-service-interface
* chore/upgrade-spring-boot-3

## **9\. Git Commit Message Convention**

⚠️ Git 커밋 메시지는 반드시 영어(English)로 작성한다.  
(응답, 설명, 문서는 한국어 / 커밋 메시지는 영어)

### **Commit Message Format**

{type}({scope}): {description}

* **scope**는 선택 사항(Optional)이나, 변경된 모듈이나 컴포넌트를 명시할 것을 권장한다.

### **Rules**

1. **Language:** English Only.
2. **Tense:** Imperative present tense (e.g., "add" not "added", "fix" not "fixed").
3. **Punctuation:** No period (.) at the end.
4. **Clarity:** Be concise but descriptive.

### **Allowed Types**

* feat : A new feature
* fix : A bug fix
* docs : Documentation only changes
* style : Changes that do not affect the meaning of the code (white-space, formatting, etc)
* refactor : A code change that neither fixes a bug nor adds a feature
* perf : A code change that improves performance
* test : Adding missing tests or correcting existing tests
* chore : Changes to the build process or auxiliary tools and libraries

### **Examples**

#### **Correct (Good)**

* feat(auth): add jwt token validation filter
* fix(order): resolve null pointer exception in calculation
* docs(readme): update installation guide
* refactor: simplify user registration flow
* chore(deps): bump spring-boot from 3.1.0 to 3.2.0

#### **Incorrect (Bad)**

* feat: 로그인 필터 추가 (Korean used)
* feat: added validation (Past tense used)
* fix: fixed bug. (Period used, vague description)

## **10\. Context & Handover Protocol (컨텍스트 보존)**

AI 세션이 단절되거나 변경되더라도 작업의 연속성을 유지하기 위해, 루트 경로의 .context.md 파일을 통해 상태를 관리한다.

### **Protocol Rules**

* **Read First:** 모든 작업 시작 전, .context.md를 읽어 현재 작업 단계(Current Task)와 남은 작업(Next Steps)을 파악한다.
* **Write Before Commit:** 커밋 직전에 개인 로그를 참고하여 .context.md를 최신 상태로 갱신한다.
* **Append History:** 기존 내용을 삭제하지 말고, 이전 상태를 날짜와 함께 요약해 History 섹션에 추가한다.
* **Structure:** .context.md는 아래의 형식을 유지해야 한다.

### **Context File Template (.context.md)**

```md
# Development Context

## 1. Current Goal (현재 목표)
- [ ] 사용자 로그인 API 구현 및 JWT 발급

## 2. Recent Changes (최근 변경 사항)
- feat: User 엔티티 생성 (YYYY-MM-DD)
- chore: Spring Security 의존성 추가

## 3. Next Steps (다음 할 일)
1. SecurityConfig 설정 클래스 작성
2. AuthController 로그인 엔드포인트 구현

## 4. Known Issues (알려진 문제/보류 사항)
- Refresh Token 저장소(Redis) 연결 정보 확인 필요

## 5. History (이전 기록)
- 2026-01-19 | 작업: 로그인 API 구현 진행 | 결과: JWT 발급 로직 추가 | 이슈: 없음
```

## **11\. Per-User Session Log (개인 로그)**

개인 로그는 협업자별 경미한 작업 기록을 위한 보조 로그이며, .context.md와 별도로 관리한다.

### **Protocol Rules**

* **Auto-Detect:** 세션 시작 시 자동으로 협업자 ID를 결정한다. 우선순위는 `git config user.email` → `git config user.name` 순이다.
* **Sanitize:** 협업자 ID는 소문자화하고, 영숫자/점/언더스코어/하이픈 이외 문자는 `-`로 치환한다.
* **Uncertainty:** ID가 비어 있거나 모호한 경우, 진행 전에 사용자 확인을 받는다.
* **Location:** 로그는 `.context.d/{collaboratorId}.md`에 저장하며 Git 추적 대상이다.
* **Creation:** 해당 파일이 없으면 템플릿을 기반으로 생성한다.
* **Stability:** 확인된 로그는 별도 요청이 없는 한 동일 로그를 세션 내 유지한다.
* **Task Logging:** 작업 단위는 개인 로그에 먼저 기록한다.
* **Context Sync:** 커밋 전에 개인 로그를 읽고 .context.md를 갱신한다.
* **Context Filter:** 로컬 선호나 로컬 환경 설정만의 변화는 .context.md에 기록하지 않는다. 단, 추후 영향을 줄 가능성이 있으면 기록한다.
* **Tracked Changes:** Git 추적 대상 파일 변경은 .context.md 업데이트 대상으로 간주하며, 중요도에 따라 간결히 요약한다.

### **Template (.context.d/{collaboratorId}.md)**

```md
# Per-User Session Log

## 1. Owner (소유자)
- id: {collaboratorId}
- source: git config user.email | git config user.name
- email: {user.email or not set}

## 2. Recent Notes (최근 메모)
- YYYY-MM-DD | 작업: ... | 결과: ... | 이슈: ...

## 3. History (이전 기록)
- YYYY-MM-DD | 작업: ... | 결과: ... | 이슈: ...
```

## **12\. Notes for Codex (Self-Check)**

작업을 완료하기 전 다음 체크리스트를 스스로 확인한다.

1. \[ \] **언어 규칙 준수:** 응답은 한국어인가? 커밋 메시지는 영어인가?
2. \[ \] **컨텍스트 동기화:** 작업 완료 후 .context.md 파일이 갱신되었는가?
3. \[ \] **안전성:** 하드코딩된 비밀번호나 민감 정보가 없는가?
4. \[ \] **완결성:** 생성된 코드가 문법 에러 없이 컴파일 가능한가?
5. \[ \] **원자성:** 하나의 커밋에 하나의 변경 사항만 담겨 있는가?

## **12\. Environment (개발 환경)**

* Java (Gradle JVM): 21.0.9
* Spring Boot: 3.5.9
* Spring Framework (spring-core): 6.2.15
* Gradle: 8.14.3
