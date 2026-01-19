# **Codex Working Guidelines (AGENTS.MD)**

이 문서는 이 저장소에서 Codex(AI Agent)가 따라야 할 작업 규칙을 정의한다.  
이 지침은 \*\*절대적(Mandatory)\*\*이며, 모든 제안과 코드 생성 시 최우선으로 적용된다.

## **0\. Meta Rules (Language Protocol)**

가장 중요한 언어 규칙은 다음과 같다.

1. **Interaction (대화 및 문서):** 모든 설명, 질문, 가이드, PR 본문은 \*\*한국어(Korean)\*\*로 작성한다.
2. **Git System (버전 관리):** 브랜치 이름과 커밋 메시지는 반드시 \*\*영어(English)\*\*로 작성한다.

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

1. **Analysis:** 변경 전 관련 파일을 읽고 전체 맥락과 의존성을 파악한다.
2. **Strategy:** 수정 범위와 영향 범위를 먼저 생각한 뒤 작업을 시작한다.
3. **Implementation:** 변경은 가능한 한 작은 단위(Atomic)로 수행한다.
4. **Verification:** 수정 후 컴파일 에러 가능성, 린트(Lint) 위반 여부를 스스로 점검한다.
5. **Suggestion:** 필요 시 간단한 테스트 방법 또는 검증 절차(cURL 예시 등)를 제안한다.

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

## **7\. 문서화 (Documentation)**

* **Sync Code & Docs:** 로직이 변경되면 주석(Javadoc/KDoc)과 관련 문서(README, API 명세)도 함께 갱신한다.
* **Tone:** 문서는 명확하고 정중한 어조(해요체 또는 건조체)를 유지한다.

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

## **10\. Notes for Codex (Self-Check)**

작업을 완료하기 전 다음 체크리스트를 스스로 확인한다.

1. \[ \] **언어 규칙 준수:** 응답은 한국어인가? 커밋 메시지는 영어인가?
2. \[ \] **안전성:** 하드코딩된 비밀번호나 민감 정보가 없는가?
3. \[ \] **완결성:** 생성된 코드가 문법 에러 없이 컴파일 가능한가?
4. \[ \] **원자성:** 하나의 커밋에 하나의 변경 사항만 담겨 있는가?