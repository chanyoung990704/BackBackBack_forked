# Posts CRUD Split Plan (Board-based System)

## 1. Current Status (Path-based Refactoring)

The codebase has been successfully refactored from a flat `/api/posts` structure to a **Board-based Pattern** where the category name is part of the URI path.

| Endpoint (Board Pattern) | Current DTO | Status |
| :--- | :--- | :--- |
| **[User]** `GET /api/posts/{categoryName}` | `PageRequest` (params) | **DONE** |
| **[User]** `GET /api/posts/{categoryName}/{postId}` | N/A | **DONE** |
| **[User]** `POST /api/posts/{categoryName}` | `PostUserCreateRequest` | **DONE** |
| **[User]** `PATCH /api/posts/{categoryName}/{postId}` | `PostUserUpdateRequest` | **DONE** |
| **[User]** `DELETE /api/posts/{categoryName}/{postId}` | N/A | **DONE** |
| **[Admin]** `GET /api/admin/posts/{categoryName}` | `PageRequest` | **DONE** |
| **[Admin]** `GET /api/admin/posts/{categoryName}/{postId}` | N/A | **DONE** |
| **[Admin]** `POST /api/admin/posts/{categoryName}` | `PostAdminCreateRequest` | **DONE** |
| **[Admin]** `PATCH /api/admin/posts/{categoryName}/{postId}` | `PostAdminUpdateRequest` | **DONE** |
| **[Admin]** `DELETE /api/admin/posts/{categoryName}/{postId}` | N/A | **DONE** |

---

## 2. Implementation Checklist

### A. Infrastructure & Data
- [x] Create Flyway migration `V16__seed_categories.sql` to seed `notices` (ID: 1) and `qna` (ID: 2).
- [x] Remove redundant `DevCategorySeeder.java` to prevent naming/logic conflicts in `dev` profile.
- [x] (Omitted per session request) Add `industry_code` column to `companies` table (upstream fix pending).

### B. DTO Refactoring
- [x] Create `PostUserCreateRequest` / `PostUserUpdateRequest` (Remove `categoryId`).
- [x] Create `PostAdminCreateRequest` / `PostAdminUpdateRequest` (Add `isPinned`, `status`).

### C. Domain & Service Layer
- [x] **PostsRepository:** Add methods to find by `categoryName` + `PostStatus` + `DeletedAtIsNull`.
- [x] **PostsEntity:** Update `update` method to support `isPinned` and `status` for admin use.
- [x] **PostService:**
    - [x] Refactor to use `categoryName` lookup.
    - [x] Implement `Admin` service methods (bypass ownership checks, full visibility).
    - [x] Enforce Board Logic:
        - `notices`: User = Read-only (GET); Admin = Full CRUD.
        - `qna`: User = Full CRUD (Own posts only, including GET); Admin = Full CRUD (All posts).

### D. API Layer (Controllers)
- [x] **PostController (`/api/posts/{categoryName}`)**: Implement user-facing board logic.
- [x] **AdminPostController (`/api/admin/posts/{categoryName}`)**: Implement admin-facing management logic.
- [x] **SecurityConfig**: Update request matchers to protect `/api/admin/posts/**` with `hasRole('ADMIN')`.

### E. Cleanup & Deprecation
- [x] Delete existing `com.aivle.project.qna` package (Controller, Service, DTOs, Mapper).
- [x] Remove any specific references to `/api/qna` in frontend-facing documentation or tests.

### F. Generate Tests
- [x] Create unit tests for `PostService` (Board logic, Admin vs User).
- [x] Create integration tests for `PostController` and `AdminPostController`.
- [x] Verify security rules in `SecurityConfig` via WebMvcTests.

---

## 3. Overview & Context Instructions

### Core Philosophy
Each category in the database is treated as a separate "Board". The board logic is determined by the `categoryName` in the URL. This generalized system **supersedes and replaces** the previous specific QnA module (`com.aivle.project.qna`). 

Security is handled via a mix of Spring Security (for the `/admin` prefix) and Service-layer logic (for ownership and board-specific rules).

### Board Rules
1. **Notices (`/notices`)**:
    - **Users**: Can only view the list and details (`GET`). Attempting to Write/Edit/Delete returns `403 Forbidden`.
    - **Admins**: Full control. Can pin posts and set status.
2. **QnA (`/qna`)**:
    - **Users**: Can create posts. Can only view, update, or delete posts **they created**.
    - **Admins**: Full control over all posts in the QnA board.

### Implementation Result
- Strictly separated User and Admin controllers.
- Ownership verified in the Service layer using the `CurrentUser` resolver.
- Verified with 27 automated tests covering all scenarios.