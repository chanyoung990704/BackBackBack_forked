ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS industry_code_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_companies_industry_code ON companies (industry_code_id);

ALTER TABLE companies
    ADD CONSTRAINT IF NOT EXISTS fk_companies_industry_code
        FOREIGN KEY (industry_code_id) REFERENCES industry_codes(id)
            ON DELETE SET NULL;
