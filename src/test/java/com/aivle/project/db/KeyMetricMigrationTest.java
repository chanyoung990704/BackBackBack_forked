package com.aivle.project.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;

import com.aivle.project.common.config.QuerydslConfig;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
class KeyMetricMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("키 메트릭 신규 테이블 및 컬럼 마이그레이션이 적용된다")
    void shouldApplyKeyMetricMigrations() {
        // given
        // when
        Integer metricDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'METRIC_DESCRIPTIONS'",
                Integer.class
        );
        Integer companyKeyMetrics = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'COMPANY_KEY_METRICS'",
                Integer.class
        );
        Integer keyMetricDescriptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'KEY_METRIC_DESCRIPTIONS'",
                Integer.class
        );

        Integer aiComment = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'COMPANY_KEY_METRICS' AND COLUMN_NAME = 'AI_COMMENT'",
                Integer.class
        );
        Integer aiSections = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'COMPANY_KEY_METRICS' AND COLUMN_NAME = 'AI_SECTIONS'",
                Integer.class
        );
        Integer signalColor = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'COMPANY_REPORT_METRIC_VALUES' AND COLUMN_NAME = 'SIGNAL_COLOR'",
                Integer.class
        );
        Integer unitColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = 'KEY_METRIC_DESCRIPTIONS' AND COLUMN_NAME = 'UNIT'",
                Integer.class
        );

        // then
        assertThat(metricDescriptions).isEqualTo(1);
        assertThat(companyKeyMetrics).isEqualTo(1);
        assertThat(keyMetricDescriptions).isEqualTo(1);
        assertThat(aiComment).isEqualTo(1);
        assertThat(aiSections).isEqualTo(1);
        assertThat(signalColor).isEqualTo(1);
        assertThat(unitColumn).isEqualTo(1);
    }
}
