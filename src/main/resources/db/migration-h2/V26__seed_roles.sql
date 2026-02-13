-- ============================================
-- 26. 역할(Role) 고정 시드 (H2)
-- ============================================

MERGE INTO roles (name, description)
KEY (name)
VALUES
    ('ROLE_USER', 'user role'),
    ('ROLE_ADMIN', 'admin role'),
    ('ROLE_ANALYST', 'analyst role');
