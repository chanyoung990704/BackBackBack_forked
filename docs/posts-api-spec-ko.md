# 게시판 기반 게시글 시스템 API 명세 (Post CRUD API Docs)

이 문서는 보드 기반 패턴(Board-based Pattern)으로 리팩토링된 게시글 CRUD API에 대한 명세입니다. 카테고리 식별은 ID가 아닌 URL 경로의 리터럴 문자열(예: `notices`, `qna`)을 사용합니다.

---

## 1. API 엔드포인트 개요

### 보드 식별자 (Path Variable)
*   `{categoryName}`: 데이터베이스에 등록된 카테고리 명칭을 그대로 사용합니다.
    *   `notices`: 공지사항 게시판
    *   `qna`: Q&A 게시판

### [사용자용] `/api/posts/{categoryName}`
일반 사용자가 게시글을 조회하거나 본인의 글을 관리할 때 사용합니다.

| 메서드 | 경로 | 설명 | 요청 DTO |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/posts/{categoryName}` | 보드별 게시글 목록 조회 | `PageRequest` (Query Param) |
| **GET** | `/api/posts/{categoryName}/{postId}` | 게시글 상세 조회 | N/A |
| **POST** | `/api/posts/{categoryName}` | 게시글 생성 | `PostUserCreateRequest` |
| **PATCH** | `/api/posts/{categoryName}/{postId}` | 게시글 수정 (본인 소유 한정) | `PostUserUpdateRequest` |
| **DELETE** | `/api/posts/{categoryName}/{postId}` | 게시글 삭제 (본인 소유 한정) | N/A |

### [관리자용] `/api/admin/posts/{categoryName}`
관리자가 공지사항을 관리하거나 모든 게시물을 제어할 때 사용합니다. (ROLE_ADMIN 권한 필요)

| 메서드 | 경로 | 설명 | 요청 DTO |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/admin/posts/{categoryName}` | 보드별 전체 목록 조회 (숨김/초안 포함) | `PageRequest` (Query Param) |
| **GET** | `/api/admin/posts/{categoryName}/{postId}` | 게시글 상세 조회 (관리자용) | N/A |
| **POST** | `/api/admin/posts/{categoryName}` | 게시글 생성 (고정/상태 설정 포함) | `PostAdminCreateRequest` |
| **PATCH** | `/api/admin/posts/{categoryName}/{postId}` | 게시글 수정 (고정/상태 설정 포함) | `PostAdminUpdateRequest` |
| **DELETE** | `/api/admin/posts/{categoryName}/{postId}` | 게시글 삭제 (영구 삭제가 아닌 Soft Delete) | N/A |

---

## 2. 보드별 특수 로직 및 권한

### **공지사항 (`notices`)**
*   **사용자**: 목록 조회 및 상세 내용 확인만 가능합니다. (**Read-only**)
    *   쓰기/수정/삭제 시도 시 `403 Forbidden`이 반환됩니다.
*   **관리자**: 게시글 작성, 상단 고정(`isPinned`), 게시 상태(`status`) 제어 등 모든 권한을 가집니다.

### **Q&A (`qna`)**
*   **사용자**: 게시글 작성이 가능하며, **본인이 작성한 글에 대해서만** 상세 조회, 수정, 삭제가 가능합니다. (**Owner-only access**)
    *   로그인하지 않았거나 타인의 비밀글/상세글에 접근 시 `403 Forbidden`이 반환됩니다.
*   **관리자**: 사용자의 소유 여부와 관계없이 모든 QnA 게시글을 열람하고 관리할 수 있습니다.

---

## 3. 상세 DTO 명세 (Swagger Schema)

### 게시글 생성 요청 (Request Body)

#### **[사용자용] `PostUserCreateRequest`**
```json
{
  "title": "게시글 제목입니다",   // 필수, 1~200자
  "content": "게시글 내용입니다"  // 필수
}
```

#### **[관리자용] `PostAdminCreateRequest`**
```json
{
  "title": "공지사항 제목",
  "content": "공지사항 내용",
  "isPinned": true,     // 상단 고정 여부 (기본값: false)
  "status": "PUBLISHED" // 게시 상태: [DRAFT, PUBLISHED, HIDDEN]
}
```

### 게시글 수정 요청 (Request Body)

#### **[사용자용] `PostUserUpdateRequest`**
```json
{
  "title": "수정할 제목",   // 선택
  "content": "수정할 내용"  // 선택
}
```

#### **[관리자용] `PostAdminUpdateRequest`**
```json
{
  "title": "수정할 제목",
  "content": "수정할 내용",
  "isPinned": false,
  "status": "HIDDEN"    // 상태값 변경 가능
}
```

### 게시글 응답 (Response Body)

#### **`PostResponse`**
```json
{
  "id": 10,
  "userId": 5,
  "categoryId": 2,
  "title": "질문있습니다",
  "content": "내용내용...",
  "viewCount": 120,
  "isPinned": false,
  "status": "PUBLISHED",
  "createdAt": "2026-02-06T15:00:00",
  "updatedAt": "2026-02-06T15:30:00"
}
```

---

## 4. 주요 예외 상황
*   `COMMON_403 (Forbidden)`: 
    *   비로그인 상태로 권한이 필요한 보드 접근 시
    *   일반 사용자가 `notices` 보드에 쓰기 시도 시
    *   일반 사용자가 타인의 `qna` 게시글 상세 조회/수정/삭제 시도 시
*   `COMMON_404 (Not Found)`:
    *   존재하지 않는 게시글 ID 접근 시
    *   게시글이 존재하더라도 URL의 `{categoryName}`과 실제 카테고리가 일치하지 않을 때
