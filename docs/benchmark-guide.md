# 벤치마크 실행 가이드

> 로컬 환경에서 JMeter 벤치마크를 실행하여 Platform Thread vs Virtual Thread, Kafka 비동기 vs 동기 모드의 성능 차이를 실측 데이터로 검증하는 가이드입니다.

---

## 1. 사전 준비

### 1.1 필수 도구 설치

| 도구 | 버전 | 용도 |
|------|------|------|
| Java (JDK) | 21+ | 애플리케이션 빌드 및 실행 |
| Apache JMeter | 5.6.3 | HTTP 부하 테스트 |
| Docker & Docker Compose | 최신 | 인프라 컨테이너 (MySQL, Redis, Kafka) |
| Bash | 4.0+ | 벤치마크 스크립트 실행 |

#### JMeter 설치

```bash
# macOS (Homebrew)
brew install jmeter

# Linux (수동 설치)
wget https://downloads.apache.org/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz -C ~/tools/
export PATH="$HOME/tools/apache-jmeter-5.6.3/bin:$PATH"

# 설치 확인
jmeter --version
```

> [!TIP]
> `jmeter` 명령어가 `PATH`에 등록되어 있어야 합니다. `run-benchmark.sh`가 `command -v jmeter`로 존재 여부를 확인합니다.

### 1.2 인프라 컨테이너 실행

벤치마크 실행 전에 MySQL, Redis, Kafka 컨테이너를 먼저 시작합니다.

```bash
# 프로젝트 루트에서
cp .env.example .env
# .env 파일을 편집하여 필요한 환경변수 설정 (DB 비밀번호, JWT 키 경로 등)

# 인프라 컨테이너 기동
docker compose up -d
```

컨테이너 상태 확인:

```bash
docker compose ps
# mysql, redis, kafka 모두 running 상태인지 확인
```

### 1.3 JWT 키 준비

```bash
mkdir -p secrets/jwt
# RSA 키 생성 (이미 있다면 생략)
openssl genrsa -out secrets/jwt/private.pem 2048
openssl rsa -in secrets/jwt/private.pem -pubout -out secrets/jwt/public.pem
```

---

## 2. 테스트 시나리오 설명

### 2.1 JMeter 테스트 플랜

**파일**: `perf/jmeter/phase1-virtual-thread-benchmark.jmx`

이 테스트 플랜은 4개의 독립적인 Thread Group으로 구성되어 있으며, Virtual Thread 도입 전/후 성능 비교를 위해 설계되었습니다.

| Thread Group | 시나리오 | 엔드포인트 | HTTP 메서드 | 설명 |
|-------------|---------|-----------|------------|------|
| **A - Signup** | 회원가입 | `POST /api/auth/signup` | POST | UUID 기반 고유 이메일로 대량 회원가입 |
| **B - News/Report Sync** | 뉴스·리포트 동기화 | `POST /api/perf/benchmark/news-sync/{stockCode}` | POST | 뉴스 수집 배치 트리거 |
| | | `POST /api/perf/benchmark/report-sync/{stockCode}` | POST | 리포트 동기화 |
| **C - Insight Refresh** | 인사이트 갱신 | `GET /api/perf/benchmark/insight-refresh/{companyId}` | GET | 비동기 인사이트 갱신 조회 |
| **D - AI Report** | AI 리포트 생성 | `POST /api/perf/benchmark/ai-report/{companyId}?year=2026&quarter=1` | POST | AI 리포트 생성 요청 |

### 2.2 기본 부하 설정

`perf/jmeter/props/common.properties` 파일에 정의된 기본 파라미터:

```properties
# 대상 서버
base.protocol=http
base.host=localhost
base.port=8080

# 부하 옵션
threads=20          # 동시 스레드 수
ramp_up=10          # Ramp-up 시간 (초)
loops=20            # 스레드당 반복 횟수

# 대상 데이터
company.id=1
company.stock.code=900001
```

> [!NOTE]
> 총 요청 수 = `threads × loops × Thread Group 수` = 20 × 20 × 4 = **1,600 요청** (기본 설정)

---

## 3. Platform Thread vs Virtual Thread 비교 실행 방법

벤치마크 스크립트는 **before**(Platform Thread) / **after**(Virtual Thread) 모드를 지원합니다.

### 3.1 전체 흐름

```
run-before.sh  →  run-benchmark.sh before  →  앱 기동(VT off) → JMeter 실행 → 결과 저장
run-after.sh   →  run-benchmark.sh after   →  앱 기동(VT on)  → JMeter 실행 → 결과 저장
compare.sh     →  before/after JTL 비교 → 비교 리포트 생성
```

### 3.2 Step-by-Step 실행

#### Step 1: Platform Thread 벤치마크 (before)

```bash
# 프로젝트 루트에서 실행
RUN_ID="$(date +%Y%m%d-%H%M%S)"

# before 모드: 모든 Virtual Thread 플래그 OFF
bash perf/scripts/run-before.sh "$RUN_ID"
```

이 스크립트가 수행하는 작업:
1. `SPRING_PROFILES_ACTIVE=perf`로 애플리케이션 기동
2. 모든 Virtual Thread 환경변수를 `false`로 설정
3. 서버 Ready 대기 (최대 180초, `Started ProjectApplication` 로그 마커 확인)
4. `collect-runtime-metrics.sh`로 런타임 메트릭 수집 시작
5. JMeter 테스트 실행
6. 결과 저장 후 서버 종료

#### Step 2: Virtual Thread 벤치마크 (after)

```bash
# 같은 RUN_ID로 after 모드 실행
bash perf/scripts/run-after.sh "$RUN_ID"
```

`after` 모드에서는 아래 환경변수가 자동으로 `true`로 설정됩니다:

| 환경변수 | 값 | 설명 |
|---------|-----|------|
| `SPRING_THREADS_VIRTUAL_ENABLED` | `true` | Spring 서블릿 Virtual Thread 활성화 |
| `APP_VIRTUAL_THREAD_ENABLED` | `true` | 앱 비동기 실행기 전체 Virtual Thread |
| `APP_VIRTUAL_THREAD_INSIGHT_ENABLED` | `true` | insightExecutor Virtual Thread |
| `APP_VIRTUAL_THREAD_EMAIL_ENABLED` | `true` | emailExecutor Virtual Thread |

#### Step 3: 결과 비교

```bash
bash perf/scripts/compare.sh "$RUN_ID"
```

### 3.3 특정 Executor만 Virtual Thread 활성화 (매트릭스 테스트)

다양한 조합을 자동으로 테스트하려면 `run-phase1-matrix.sh`를 사용합니다:

```bash
bash perf/scripts/run-phase1-matrix.sh "20260521-vt-scope"
```

이 스크립트는 4가지 조합을 순차 실행합니다:

| Case 이름 | SPRING_THREADS | APP_GLOBAL | INSIGHT | EMAIL |
|-----------|:---:|:---:|:---:|:---:|
| `all-on` | ✅ | ✅ | ✅ | ✅ |
| `insight-only` | ✅ | ❌ | ✅ | ❌ |
| `email-only` | ✅ | ❌ | ❌ | ✅ |
| `global-only` | ✅ | ✅ | ❌ | ❌ |

각 조합마다 `before` → `after` → `compare` 를 자동으로 수행합니다.

### 3.4 개별 Executor 수동 오버라이드

특정 플래그만 변경하여 `after` 모드를 실행하려면:

```bash
# insightExecutor만 Virtual Thread 활성화
APP_VIRTUAL_THREAD_ENABLED=false \
APP_VIRTUAL_THREAD_INSIGHT_ENABLED=true \
APP_VIRTUAL_THREAD_EMAIL_ENABLED=false \
SPRING_THREADS_VIRTUAL_ENABLED=true \
bash perf/scripts/run-after.sh "$RUN_ID"
```

---

## 4. Kafka 비동기 vs 동기 비교 실행 방법

AI 리포트 생성 경로를 Kafka 비동기 / 동기 WebClient 간에 전환하여 비교합니다.

### 4.1 동기 모드 (Kafka 비활성)

```bash
RUN_ID_SYNC="$(date +%Y%m%d-%H%M%S)-kafka-sync"

APP_AI_JOB_KAFKA_ENABLED=false \
SPRING_PROFILES_ACTIVE=perf \
./gradlew bootRun --no-daemon &
SERVER_PID=$!

# 서버 준비 대기
until curl -sf http://localhost:8080/api/perf/benchmark/fixture > /dev/null; do sleep 1; done

# JMeter 실행 (AI Report 시나리오만)
mkdir -p perf/results/${RUN_ID_SYNC}/sync
jmeter -n \
  -t perf/jmeter/phase1-virtual-thread-benchmark.jmx \
  -q perf/jmeter/props/common.properties \
  -Jcompany.id=1 \
  -Jcompany.stock.code=900001 \
  -l perf/results/${RUN_ID_SYNC}/sync/results.jtl \
  -e -o perf/results/${RUN_ID_SYNC}/sync/html-report

kill $SERVER_PID
```

### 4.2 비동기 모드 (Kafka 활성)

```bash
APP_AI_JOB_KAFKA_ENABLED=true \
SPRING_PROFILES_ACTIVE=perf \
./gradlew bootRun --no-daemon &
SERVER_PID=$!

until curl -sf http://localhost:8080/api/perf/benchmark/fixture > /dev/null; do sleep 1; done

mkdir -p perf/results/${RUN_ID_SYNC}/async
jmeter -n \
  -t perf/jmeter/phase1-virtual-thread-benchmark.jmx \
  -q perf/jmeter/props/common.properties \
  -Jcompany.id=1 \
  -Jcompany.stock.code=900001 \
  -l perf/results/${RUN_ID_SYNC}/async/results.jtl \
  -e -o perf/results/${RUN_ID_SYNC}/async/html-report

kill $SERVER_PID
```

> [!IMPORTANT]
> Kafka 비동기 모드 테스트 시 Kafka 컨테이너(`docker compose up kafka`)가 반드시 실행 중이어야 합니다. 동기 모드에서는 AI 서버(또는 Stub)가 필요합니다.

---

## 5. 결과 수집 및 분석 방법

### 5.1 결과 디렉터리 구조

벤치마크 실행 후 결과는 `perf/results/<RUN_ID>/` 하위에 저장됩니다:

```
perf/results/<RUN_ID>/
├── before/
│   ├── results.jtl                    # JMeter 원시 결과 (CSV)
│   ├── html-report/                   # JMeter HTML 리포트
│   ├── server.log                     # 애플리케이션 서버 로그
│   ├── jmeter.log                     # JMeter 실행 로그
│   ├── runtime-metrics.csv            # Actuator 런타임 메트릭 (1초 간격)
│   ├── runtime-metrics-summary.md     # 메트릭 요약 (Markdown)
│   ├── runtime-metrics-summary.tsv    # 메트릭 요약 (TSV, compare용)
│   ├── runtime-metrics.log            # 메트릭 수집 로그
│   └── run-meta.env                   # 실행 환경 메타데이터
├── after/
│   └── (동일 구조)
├── comparison-summary.md              # Before vs After 비교 리포트
├── comparison-api-delta.csv           # API 지표 변화량 CSV
└── comparison-runtime-delta.csv       # 런타임 지표 변화량 CSV
```

### 5.2 compare.sh 사용법

`compare.sh`는 before/after 결과를 비교하여 3개의 파일을 생성합니다:

```bash
bash perf/scripts/compare.sh <RUN_ID>
```

#### 생성되는 파일

| 파일 | 내용 |
|------|------|
| `comparison-summary.md` | Before/After 테이블 + API Delta + Runtime Metrics Delta (Markdown) |
| `comparison-api-delta.csv` | API별 `avg(ms)`, `p95(ms)`, `error%`, `tps` 변화율 |
| `comparison-runtime-delta.csv` | CPU, 스레드 수, HikariCP, GC, Circuit Breaker 등 변화율 |

#### 비교 리포트에 포함되는 지표

**API 지표:**

| 항목 | 설명 |
|------|------|
| `samples` | 총 요청 수 |
| `error%` | 에러율 |
| `avg(ms)` | 평균 응답 시간 |
| `p95(ms)` | 95 백분위 응답 시간 |
| `tps` | 초당 처리량 |

**런타임 메트릭** (`collect-runtime-metrics.sh`가 Actuator에서 수집):

| 메트릭 | 소스 |
|--------|------|
| `process_cpu_usage` / `system_cpu_usage` | `/actuator/metrics/process.cpu.usage` |
| `jvm_threads_live` | `/actuator/metrics/jvm.threads.live` |
| `hikari_active` / `hikari_pending` | `/actuator/metrics/hikaricp.connections.*` |
| `tomcat_threads_busy` | `/actuator/metrics/tomcat.threads.busy` |
| `gc_pause_count` / `gc_pause_total_time` | `/actuator/metrics/jvm.gc.pause` |
| `cb_success` / `cb_failure` / `cb_slow` | `/actuator/metrics/resilience4j.circuitbreaker.calls` |
| `retry_*` | `/actuator/metrics/resilience4j.retry.calls` |
| `bulkhead_*` | `/actuator/metrics/resilience4j.bulkhead.calls` |

### 5.3 JMeter HTML 리포트 보기

```bash
# 브라우저에서 열기
open perf/results/<RUN_ID>/before/html-report/index.html
open perf/results/<RUN_ID>/after/html-report/index.html
```

---

## 6. 성능 리포트 업데이트 방법

실측 수치를 수집한 후 `docs/performance-report.md`의 추정치를 실측 데이터로 교체합니다.

### 6.1 교체 대상 확인

`docs/performance-report.md` 상단에 다음 안내가 있습니다:

> 본 리포트의 수치는 프로젝트 아키텍처 및 워크로드 특성에 기반한 **합리적 추정치**입니다.

### 6.2 교체 절차

#### Step 1: 벤치마크 실행 및 결과 수집

```bash
# 전체 벤치마크 실행
RUN_ID="$(date +%Y%m%d-%H%M%S)-production"
bash perf/scripts/run-before.sh "$RUN_ID"
bash perf/scripts/run-after.sh "$RUN_ID"
bash perf/scripts/compare.sh "$RUN_ID"
```

#### Step 2: 비교 리포트에서 수치 추출

```bash
# 비교 요약 확인
cat perf/results/$RUN_ID/comparison-summary.md

# API Delta CSV에서 주요 지표 확인
cat perf/results/$RUN_ID/comparison-api-delta.csv
```

#### Step 3: performance-report.md 교체

비교 리포트의 실측 수치로 `docs/performance-report.md`의 다음 섹션들을 업데이트합니다:

| 섹션 | 교체할 데이터 |
|------|-------------|
| §3 Virtual Thread 비교 | `comparison-summary.md`의 Before/After 테이블 |
| §4 Kafka 비동기 비교 | 동기/비동기 JTL에서 추출한 응답시간·TPS |
| §6 종합 요약 | 전체 비교 delta 수치 |

#### Step 4: 안내 문구 제거

실측 데이터 반영 후 상단의 추정치 안내를 제거하거나 다음으로 교체:

```markdown
> [!NOTE]
> 본 리포트의 수치는 YYYY-MM-DD 실측 벤치마크 결과입니다.
> 테스트 환경: [하드웨어 사양 기술]
> RUN_ID: <RUN_ID>
```

---

## 7. Grafana 대시보드에서 스크린샷 캡처 방법

### 7.1 Prometheus + Grafana 실행

`docker-compose.local.yml`에 Prometheus와 Grafana가 이미 정의되어 있습니다:

```bash
# 앱 + 인프라 + 모니터링 전체 기동
cp .env.example .env
APP_IMAGE=local-backbackback:dev \
APP_ENV_FILE=.env \
LOCAL_ENV_FILE=.env \
docker compose -f docker-compose.app.yml -f docker-compose.local.yml up -d
```

또는 모니터링만 별도로 기동:

```bash
docker compose -f docker-compose.local.yml up -d prometheus grafana
```

> [!NOTE]
> Prometheus는 `monitoring/prometheus/prometheus.yml` 설정을 사용하여 `app:8080`의 `/actuator/prometheus` 엔드포인트를 15초 간격으로 스크래핑합니다.

### 7.2 대시보드 접속

| 서비스 | URL | 기본 인증 |
|--------|-----|----------|
| **Grafana** | `http://localhost:3000` | `admin` / `admin` |
| **Prometheus** | `http://localhost:9090` | 인증 없음 |

Grafana 대시보드는 프로비저닝으로 자동 설정됩니다:
- **대시보드 JSON**: `monitoring/grafana/dashboards/spring-boot-dashboard.json`
- **Datasource**: `monitoring/grafana/provisioning/datasources/prometheus.yml`
- **Dashboard Provider**: `monitoring/grafana/provisioning/dashboards/dashboard.yml`

### 7.3 주요 대시보드 패널

`spring-boot-dashboard.json`에 정의된 주요 패널을 활용하여 벤치마크 중 시스템 상태를 모니터링합니다:

| 패널 | 확인 포인트 |
|------|-----------|
| JVM Threads | Platform Thread 수 vs Virtual Thread 수 변화 |
| CPU Usage | before/after 간 CPU 사용률 비교 |
| HikariCP Connections | DB 커넥션 풀 활성/대기 수 |
| GC Pause | GC 빈도 및 정지 시간 |
| HTTP Request Duration | API 응답시간 분포 |
| Resilience4j Circuit Breaker | Circuit 상태 전환 이력 |

### 7.4 스크린샷 캡처

#### 방법 1: Grafana Rendering API (추천)

```bash
# Grafana Rendering 플러그인 설치 (Docker 환경에서)
docker exec -it project-grafana-local grafana-cli plugins install grafana-image-renderer
docker restart project-grafana-local

# API로 대시보드 스크린샷 캡처
# 대시보드 UID는 Grafana UI에서 확인 (Settings → JSON Model → uid 값)
DASHBOARD_UID="<대시보드-uid>"
GRAFANA_URL="http://localhost:3000"

curl -o dashboard-screenshot.png \
  "${GRAFANA_URL}/render/d/${DASHBOARD_UID}?orgId=1&width=1920&height=1080&from=now-1h&to=now" \
  -u admin:admin
```

#### 방법 2: Grafana UI에서 수동 캡처

1. `http://localhost:3000` 접속 → 대시보드 선택
2. 벤치마크 시간 범위를 Time Picker에서 설정
3. **Share Dashboard** (상단 공유 아이콘) → **Snapshot** → **Local Snapshot** 으로 저장
4. 또는 각 패널의 **⋮** 메뉴 → **Share** → **Direct link rendered image** 클릭

#### 방법 3: Grafana 패널별 PNG Export

```bash
# 특정 패널 캡처 (panelId는 대시보드 JSON에서 확인)
PANEL_ID=2
curl -o panel-${PANEL_ID}.png \
  "${GRAFANA_URL}/render/d-solo/${DASHBOARD_UID}?orgId=1&panelId=${PANEL_ID}&width=800&height=400&from=now-1h&to=now" \
  -u admin:admin
```

### 7.5 벤치마크 전체 워크플로 (모니터링 포함)

```bash
# 1. 전체 환경 기동
docker compose -f docker-compose.app.yml -f docker-compose.local.yml up -d

# 2. Grafana 대시보드 확인 (http://localhost:3000)

# 3. 벤치마크 실행 (별도 터미널)
RUN_ID="$(date +%Y%m%d-%H%M%S)"
bash perf/scripts/run-before.sh "$RUN_ID"
bash perf/scripts/run-after.sh "$RUN_ID"
bash perf/scripts/compare.sh "$RUN_ID"

# 4. Grafana에서 벤치마크 시간 범위로 필터링하여 스크린샷 캡처

# 5. 결과 확인
cat perf/results/$RUN_ID/comparison-summary.md

# 6. 환경 종료
docker compose -f docker-compose.app.yml -f docker-compose.local.yml down
```

---

## 부록: 스크립트 레퍼런스

| 스크립트 | 용도 | 사용법 |
|---------|------|--------|
| `perf/scripts/run-benchmark.sh` | 벤치마크 핵심 스크립트 (앱 기동 → JMeter → 종료) | `run-benchmark.sh <before\|after> [run_id]` |
| `perf/scripts/run-before.sh` | Platform Thread 벤치마크 래퍼 | `run-before.sh [run_id]` |
| `perf/scripts/run-after.sh` | Virtual Thread 벤치마크 래퍼 | `run-after.sh [run_id]` |
| `perf/scripts/compare.sh` | before/after 결과 비교 | `compare.sh <run_id>` |
| `perf/scripts/run-phase1-matrix.sh` | 4가지 VT 조합 자동 실행 | `run-phase1-matrix.sh <base_run_id>` |
| `perf/scripts/collect-runtime-metrics.sh` | Actuator 메트릭 수집 (1초 간격) | `collect-runtime-metrics.sh <output_csv> [interval_sec]` |
| `perf/scripts/summarize-runtime-metrics.sh` | 메트릭 CSV → 요약 MD/TSV | `summarize-runtime-metrics.sh <input_csv> <output_md> <output_tsv>` |
