ALTER TABLE users DROP FOREIGN KEY fk_users_company;
ALTER TABLE users DROP INDEX idx_users_company;
ALTER TABLE users DROP COLUMN company_id;
