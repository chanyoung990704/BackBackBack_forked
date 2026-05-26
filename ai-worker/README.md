# Python Mock AI Worker 마이크로서비스

이 마이크로서비스는 대용량 트래픽 및 Low Coupling 아키텍처 하에서, AI 리포트 생성 프로세스를 비동기 분산형으로 수행하는 **Python Mock AI Worker**입니다.  
FastAPI 헬스체크 서버와 백그라운드 Kafka Consumer/Producer 스레드가 조화롭게 구동되어, 자바 백엔드의 Saga Orchestrator에서 발송하는 AI 요청들을 비동기식으로 안정되게 분산 처리합니다.

---

## 1. 주요 기능
- **FastAPI 초경량 헬스체크**: 포트 `8082`로 `/health` 엔드포인트를 열어 서비스 상태(`UP`/`DOWN`)를 확인 및 모니터링합니다.
- **백그라운드 Kafka 통신 스레드**: 데몬(Daemon) 스레드에서 `confluent-kafka`를 구동하여 `ai-job-request` 토픽을 실시간으로 구독합니다.
- **이종 언어 간 완벽한 필드 동기화**: Java 백엔드의 `AiJobMessage` 레코드 스펙을 반영하여 JSON 포맷 예외를 철저히 방지하고 강건하게 파싱합니다.
- **재무 분석 예측 시뮬레이션 (`AI_FINANCIAL_ANALYSIS`)**:
  - 3초 지연(작업 시간 모사) 후, 예측 수치(ROA, ROE, DEBT_RATIO) 맵을 가공하여 `ai-job-response` 토픽으로 응답을 발행합니다.
- **종합 코멘트 PDF 자동 빌드 (`AI_COMMENT_COMPILATION`)**:
  - 3초 지연 후, `fpdf2` 라이브러리를 통해 온전하고 프리미엄한 금융 분석 리포트 PDF 문서를 로컬 디렉토리에 동적으로 빌드합니다.
  - 저장 위치: `~/uploads/reports/{stockCode}/{year}/{quarter}/report_{stockCode}_{year}_{quarter}.pdf`  
    (파이썬의 `os.path.expanduser("~")`를 활용하여 Windows 및 Linux 환경에서 공통적으로 홈 디렉토리 내의 `uploads` 폴더를 안전하게 생성합니다.)
  - 생성 완료 후, `ai-job-response` 토픽으로 저장 메타데이터(`storageKey`, `filename`)와 함께 성공 응답을 발행합니다.

---

## 2. 디렉토리 구조
```directory
ai-worker/
├── requirements.txt   # 라이브러리 의존성 파일
├── main.py           # FastAPI 서버 및 백그라운드 Kafka Consumer/Producer 메인 코드
├── setup_env.ps1     # Windows용 가상환경 셋업 및 패키지 자동 설치 스크립트
├── setup_env.sh      # Linux/Mac용 가상환경 셋업 및 패키지 자동 설치 스크립트
└── README.md         # 서비스 사용 설명서 (본 파일)
```

---

## 3. 로컬 가상환경 설정 및 실행 가이드

### A. Windows 환경
1. PowerShell 콘솔을 엽니다.
2. `ai-worker` 디렉토리로 이동한 뒤, 셋업 스크립트를 실행합니다:
   ```powershell
   .\setup_env.ps1
   ```
3. 셋업이 완료되면 가상환경(venv)의 Python 인터프리터로 서버를 실행합니다:
   ```powershell
   & venv\Scripts\python.exe main.py
   ```

### B. Linux / Mac 환경
1. 터미널을 엽니다.
2. `ai-worker` 디렉토리로 이동한 뒤, 실행 권한을 부여하고 셋업 스크립트를 실행합니다:
   ```bash
   chmod +x setup_env.sh
   ./setup_env.sh
   ```
3. 가상환경을 활성화하고 서버를 구동합니다:
   ```bash
   source venv/bin/activate
   python main.py
   ```

> [!NOTE]  
> 이 서비스는 로컬에 Kafka 브로커(`localhost:9092`)가 실행 중인 상태를 기대합니다.  
> 만약 카프카가 켜져 있지 않으면 경고 로그를 반복해서 출력하지만, 헬스체크 API(`/health`)는 `8082` 포트에서 정상적으로 동작합니다.

---

## 4. Kafka 메시지 프로토콜 스펙 (Java 동기화)

### A. AI 작업 요청 메시지 (Request - `ai-job-request` 토픽)
Java 백엔드의 `AiJobMessage` 레코드에서 직렬화되어 들어오는 JSON 형식입니다:
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "AI_FINANCIAL_ANALYSIS",
  "companyId": 20,
  "year": 2026,
  "quarter": 1,
  "period": null,
  "requestedAt": "2026-05-26T12:17:50+09:00"
}
```
- **requestId**: 요청 고유 UUID (String)
- **type**: 작업 분류 (`AI_FINANCIAL_ANALYSIS` 또는 `AI_COMMENT_COMPILATION`)
- **companyId**: 기업 고유 ID (Long)
- **year / quarter**: 대상 연도 및 분기 (Integer)
- **period**: 데이터 대상 구간 (String, Option)
- **requestedAt**: 요청 일시 (ISO-8601 OffsetDateTime)

---

### B. AI 작업 결과 메시지 (Response - `ai-job-response` 토픽)
Mock AI Worker가 수행을 끝내고 Java 백엔드로 돌려주는 JSON 형식입니다.

#### 1) `AI_FINANCIAL_ANALYSIS` 성공 응답
```json
{
  "success": true,
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "AI_FINANCIAL_ANALYSIS",
  "predictions": {
    "ROA": 4.2,
    "ROE": 7.8,
    "DEBT_RATIO": 120.5
  }
}
```
- **success**: 성공 여부 (`true` 고정)
- **predictions**: 재무 예측 수치 맵 (`ROA`, `ROE`, `DEBT_RATIO` 필수 포함)

#### 2) `AI_COMMENT_COMPILATION` 성공 응답
```json
{
  "success": true,
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "AI_COMMENT_COMPILATION",
  "storageKey": "reports/000020/2026/1/report_000020_2026_1.pdf",
  "filename": "report_000020_2026_1.pdf"
}
```
- **success**: 성공 여부 (`true` 고정)
- **storageKey**: 웹서버 및 스토리지의 파일 경로 구조 포맷 (`reports/{stockCode}/{year}/{quarter}/{filename}`)
- **filename**: 실제 생성 및 저장된 PDF 파일명

#### 3) 작업 처리 실패 시 에러 응답 (공통)
```json
{
  "success": false,
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "AI_FINANCIAL_ANALYSIS",
  "errorMessage": "Connection timeout to model engine."
}
```
- **success**: 실패 여부 (`false` 고정)
- **errorMessage**: 작업 도중 발생한 예외 에러 상세 메시지

---

## 5. 생성되는 PDF 디자인 명세
`fpdf2` 라이브러리로 생성되는 리포트 PDF는 단순 덤프 파일이 아닌 **프리미엄 레벨의 시각적 요소**가 접목되어 있습니다.
1. **Sleek Slate Dark 테두리**: 세련되고 프로페셔널한 기업용 문서 느낌의 상하 장식 레이아웃 적용.
2. **정렬된 메타데이터 요약 박스**: 대상 기업, 분기, 일시, 분석 결과 완료 여부 표시.
3. **지표 예측치 테이블화**: 데이터의 시인성을 최대화하기 위해 깔끔하게 선이 그려진 테이블 격자 형태로 출력.
4. **AI 인사이트 코멘트**: 예측 수치와 결부되어 실제 금융 전문가의 분석 의견서와 유사한 구성의 다중 문단(Multi-paragraph) 생성.
5. **법적 면책 경고창**: 금색/황색 배경의 워닝 박스를 그려 문서의 완성도와 전문성을 Wow 요소로 강조.
