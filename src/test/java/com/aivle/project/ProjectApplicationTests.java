package com.aivle.project;

import com.aivle.project.common.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 컨텍스트 로드 스모크 테스트.
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
class ProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
