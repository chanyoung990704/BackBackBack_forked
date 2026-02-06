-- ============================================
-- 17. 게시판 카테고리 초기 데이터 (H2용)
-- ============================================

MERGE INTO categories (id, name, description, sort_order, is_active)
KEY (id)
VALUES
    (1, 'notices', '공지사항 게시판', 1, TRUE),
    (2, 'qna', 'Q&A 게시판', 2, TRUE);
