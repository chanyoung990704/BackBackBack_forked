CREATE TABLE news_analyses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  company_name VARCHAR(255) NOT NULL,
  total_count INT NOT NULL DEFAULT 0,
  average_score DECIMAL(10,6),
  analyzed_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_news_analysis_company FOREIGN KEY (company_id)
    REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_news_analysis_company ON news_analyses(company_id, analyzed_at);
CREATE INDEX idx_news_analysis_timestamp ON news_analyses(analyzed_at);

CREATE TABLE news_articles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  news_analysis_id BIGINT NOT NULL,
  title VARCHAR(500) NOT NULL,
  summary VARCHAR(10000),
  score DECIMAL(10,6),
  published_at TIMESTAMP,
  link VARCHAR(2000) NOT NULL,
  sentiment VARCHAR(10) NOT NULL DEFAULT 'NEU',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT fk_news_article_analysis FOREIGN KEY (news_analysis_id)
    REFERENCES news_analyses(id) ON DELETE CASCADE
);

CREATE INDEX idx_news_article_analysis ON news_articles(news_analysis_id);
CREATE INDEX idx_news_article_published ON news_articles(published_at);
CREATE INDEX idx_news_article_sentiment ON news_articles(sentiment);
