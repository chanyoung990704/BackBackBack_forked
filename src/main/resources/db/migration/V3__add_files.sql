-- 파일 테이블 생성
CREATE TABLE files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    storage_url VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    created_by BIGINT,
    updated_by BIGINT,
    INDEX idx_files_post_id (post_id),
    INDEX idx_files_deleted_at (deleted_at),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
