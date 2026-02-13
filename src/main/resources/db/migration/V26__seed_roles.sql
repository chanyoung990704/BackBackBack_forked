-- ============================================
-- 26. 역할(Role) 고정 시드
-- ============================================

INSERT INTO `roles` (`name`, `description`)
VALUES
    ('ROLE_USER', 'user role'),
    ('ROLE_ADMIN', 'admin role'),
    ('ROLE_ANALYST', 'analyst role')
ON DUPLICATE KEY UPDATE
    `description` = VALUES(`description`);
