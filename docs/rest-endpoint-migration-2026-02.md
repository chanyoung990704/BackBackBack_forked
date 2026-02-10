# REST 엔드포인트 정규화 매핑 (2026-02)

## 개요
- 목적: 컨트롤러 URI를 리소스 중심으로 정리하고, 기존 경로는 하위호환으로 병행 지원합니다.
- 범위: Post, Company, AI Report, News, Report Analysis, File, Watchlist

## 매핑표 (Old -> New)

### Post
- `GET /api/posts/{categoryName}` -> `GET /api/posts?categoryName={categoryName}`
- `GET /api/posts/{categoryName}/{postId}` -> `GET /api/posts/{postId}`
- `POST /api/posts/{categoryName}` -> `POST /api/posts` (body에 `categoryName` 포함)
- `PATCH /api/posts/{categoryName}/{postId}` -> `PATCH /api/posts/{postId}`
- `DELETE /api/posts/{categoryName}/{postId}` -> `DELETE /api/posts/{postId}`

### Company / Overview / Search
- `GET /api/companies/search?keyword=...` -> `GET /api/companies/search?query=...` (`keyword`도 계속 지원)
- `GET /api/companies` -> 유지
- `GET /api/companies/me` -> 신규(내 워치리스트 기업 조회)
- `GET /api/companies/{companyId}/overview` -> `GET /api/companies/{companyId}`

### Company AI
- `GET /api/companies/{companyId}/ai-analysis` -> `GET /api/companies/{companyId}/analysis`
- `POST /api/companies/{companyId}/ai-report/request` -> `POST /api/companies/{companyId}/ai-reports/requests`
- `GET /api/companies/{companyId}/ai-report/status/{requestId}` -> `GET /api/companies/{companyId}/ai-reports/requests/{requestId}`
- `POST /api/companies/{companyId}/ai-report` -> `POST /api/companies/{companyId}/ai-reports`
- `GET /api/companies/{companyId}/ai-report/download` -> `GET /api/companies/{companyId}/ai-reports/file`

### News
- `POST /api/companies/{companyId}/news/fetch` -> `POST /api/companies/{companyId}/news/sync`
- `POST /api/companies/{companyId}/news/refresh-latest` -> `POST /api/companies/{companyId}/news/refresh`
- `GET /api/companies/{companyId}/news/history` -> `GET /api/companies/{companyId}/news`
- `GET /api/companies/{companyId}/news/latest` -> 유지

### Report Analysis
- `POST /api/companies/{companyId}/report/fetch` -> `POST /api/companies/{companyId}/reports/sync`
- `GET /api/companies/{companyId}/report/latest` -> `GET /api/companies/{companyId}/reports/latest`

### File
- `GET /api/files/{fileId}/url` -> `GET /api/files/{fileId}/download-url`

### Watchlist
- `GET /api/watchlists/metric-values` -> `GET /api/watchlists/metrics/values`

### Admin (User Context Query)
- `GET /api/admin/users` -> 활성(`ACTIVE`) + 미삭제 사용자 목록(`id`, `name`, `email`) 조회
- `GET /api/admin/dashboard/summary?userId=...` -> 특정 사용자 기준 대시보드 요약 조회
- `GET /api/admin/companies?userId=...` -> 특정 사용자 워치리스트 기업 목록 조회
- `GET /api/admin/companies/{id}/overview?userId=...` -> 특정 사용자 기준 기업 개요 조회
- `GET /api/admin/companies/{id}/insights?userId=...` -> 특정 사용자 기준 기업 인사이트 조회

#### Admin 사용자 조회 규칙
- `userId`는 대상 사용자 존재 여부 + `ACTIVE` 상태 + 미삭제(`deleted_at IS NULL`)를 검증합니다.
- `overview/insights`는 사용자-기업 소속 여부를 강제하지 않고, 사용자 유효성만 확인합니다.

## 하위호환 정책
- 위 Old 경로는 현재 릴리즈에서 계속 동작하도록 유지합니다.
- 신규 개발/프론트 연동은 New 경로 사용을 권장합니다.
- 구 경로 제거 시점은 별도 릴리즈 노트로 공지합니다.
