# Post List User Filtering Implementation Plan

## 1. Objective
Enhance the security and efficiency of the QnA board by moving the "My Posts" filtering logic to the backend. 
- **Target**: QnA Board (`/qna`) only.
- **Mechanism**: Strictly filter by the authenticated user's ID (from JWT).
- **Constraints**: No changes to DTOs or API request parameters (Query/Body).

---

## 2. Impact Assessment

| Component | File Path | Impact |
| :--- | :--- | :--- |
| **Repository** | `PostsRepository.java` | **Medium**: Add paginated query method `findAllByCategoryNameAndUserIdAndStatus...`. |
| **Service** | `PostService.java` | **High**: Update `list` logic to apply user-filtering strictly for the `qna` category. |
| **Controller** | `PostController.java` | **None**: The existing signature already receives `UserEntity user`. |
| **Tests** | `PostServiceTest.java`, `PostControllerIntegrationTest.java` | **High**: Update/Add tests to verify that QnA listing is restricted to the current user's ID. |

---

## 3. Implementation Steps & Checklist

### A. Repository Enhancements
- [x] **PostsRepository.java**:
    - [x] Add `findAllByCategoryNameAndUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc` with `Pageable` support.

### B. Service Layer Refactoring
- [x] **PostService.java**:
    - [x] Update the `list` method logic to filter by `user.getId()` for QnA and keep notices public.

### C. Creating Tests
- [ ] **PostServiceTest.java**:
    - [ ] Add unit tests to verify that for `qna`, the repository is called with the specific `userId`.
    - [ ] Verify that for `notices`, the repository is called without a `userId` filter.
- [ ] **PostControllerIntegrationTest.java**:
    - [ ] Create/Update integration tests for `GET /api/posts/qna`.
    - [ ] Use credentials and JWT tokens from `secrets.log` for authentication.
    - [ ] Verify only posts belonging to the authenticated user are returned.

### D. Running & Verifying Tests
- [ ] **Test Execution**:
    - [ ] Run `./gradlew test --tests "com.aivle.project.post.*"` to verify post-domain logic.
    - [ ] Run full test suite if necessary to ensure no regressions.

---

## 4. Execution Protocol

1.  **Acknowledge Checklist**: State the full checklist explicitly before starting implementation.
2.  **Iterative Implementation**: Work through items C and D.
3.  **Confirm Compliance**: Final statement confirming all steps were followed faithfully.
4.  **Version Control**: Commit with appropriate messages for each step.
5.  **Stop**: Wait for confirmation before any next steps.

---

## 5. Security & Testing Note
- **JWT Integrity**: Authentication in integration tests will rely on valid JWT tokens or the configured security context mimicking them, using parameters provided in `secrets.log`.
- **Privacy Enforcement**: By relying exclusively on the JWT for filtering the QnA list, we eliminate the risk of ID spoofing.
