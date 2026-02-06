-- ============================================
-- 17. 게시판 카테고리 초기 데이터
-- ============================================

INSERT INTO `categories` (name, description, sort_order, is_active)
VALUES
    ('notices', '공지사항 게시판', 1, 1),
    ('qna', 'Q&A 게시판', 2, 1)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active);
