package com.aivle.project.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

	@Test
	@DisplayName("OpenAPI 서버 URL은 상대 경로(/)로 고정한다")
	void openApiServerUrlShouldBeRelativePath() {
		// given
		OpenApiConfig config = new OpenApiConfig("v1");

		// when
		OpenAPI openAPI = config.openAPI();

		// then
		assertThat(openAPI.getServers()).isNotNull();
		assertThat(openAPI.getServers()).hasSize(1);
		assertThat(openAPI.getServers().getFirst().getUrl()).isEqualTo("/");
	}
}
