-- ============================================
-- 17. 게시판 카테고리 초기 데이터
-- ============================================

INSERT INTO `categories` (id, name, description, sort_order, is_active)
VALUES
    (1, 'notices', '공지사항 게시판', 1, 1),
    (2, 'qna', 'Q&A 게시판', 2, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);
