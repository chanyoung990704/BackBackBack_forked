# Phase1 Virtual Thread JMeter 벤치마크 가이드

## 1. 목적

- Virtual Thread 도입 전(`before`)과 도입 후(`after`) 성능을 동일 조건으로 비교합니다.
- 비교 기준은 JMeter JTL/HTML 리포트와 `comparison-summary.md`입니다.

## 2. 준비 사항

1. `jmeter` CLI가 설치되어 있어야 합니다.
2. 로컬에서 `./gradlew` 실행이 가능해야 합니다.
3. 포트 `8080`이 비어 있어야 합니다.

## 3. 실행 절차

1. Before 측정

```bash
./perf/scripts/run-before.sh 20260211-vt-phase1
```

2. After 측정

```bash
./perf/scripts/run-after.sh 20260211-vt-phase1
```

3. 비교 리포트 생성

```bash
./perf/scripts/compare.sh 20260211-vt-phase1
```

## 4. 산출물 경로

- `perf/results/<run_id>/before/results.jtl`
- `perf/results/<run_id>/before/html-report/`
- `perf/results/<run_id>/before/runtime-metrics.csv`
- `perf/results/<run_id>/before/runtime-metrics-summary.md`
- `perf/results/<run_id>/after/results.jtl`
- `perf/results/<run_id>/after/html-report/`
- `perf/results/<run_id>/after/runtime-metrics.csv`
- `perf/results/<run_id>/after/runtime-metrics-summary.md`
- `perf/results/<run_id>/comparison-summary.md`

## 5. 시나리오

- `A - Signup`: `POST /api/auth/signup`
- `B - News/Report Sync`: `POST /api/perf/benchmark/news-sync/{stockCode}`, `POST /api/perf/benchmark/report-sync/{stockCode}`
- `C - Insight Refresh`: `GET /api/perf/benchmark/insight-refresh/{companyId}`
- `D - AI Report`: `POST /api/perf/benchmark/ai-report/{companyId}`

## 6. 주의 사항

- `perf` 프로파일은 외부 AI/Turnstile 의존성을 mock 처리합니다.
- `run-before.sh`/`run-after.sh`는 내부적으로 서버를 기동/종료합니다.
- 결과 비교는 동일 `run_id`를 사용해야 합니다.
- `comparison-summary.md`에는 응답시간/TPS와 함께 런타임 계측(before/after) 비교가 포함됩니다.
