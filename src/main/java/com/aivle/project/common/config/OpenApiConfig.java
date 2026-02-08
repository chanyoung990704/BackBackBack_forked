package com.aivle.project.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Swagger/OpenAPI 설정.
 */
@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";
	private final String apiVersion;

	public OpenApiConfig(
		@Value("${app.api.version:v1}") String apiVersion
	) {
		this.apiVersion = apiVersion;
	}

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Project API")
				.description("Project API 문서")
				.version(apiVersion))
			.components(new Components()
				.addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
					.name(SECURITY_SCHEME_NAME)
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
			.addServersItem(new Server().url("/"));
	}

	@Bean
	public GroupedOpenApi adminApi() {
		return GroupedOpenApi.builder()
			.group("admin")
			.packagesToScan(
				"com.aivle.project.company.controller",
				"com.aivle.project.report.controller",
				"com.aivle.project.metricaverage.controller"
			)
			.build();
	}

	@Bean
	public GroupedOpenApi authApi() {
		return GroupedOpenApi.builder()
			.group("auth")
			.packagesToScan("com.aivle.project.auth.controller")
			.build();
	}

	@Bean
	public GroupedOpenApi postApi() {
		return GroupedOpenApi.builder()
			.group("post")
			.packagesToScan("com.aivle.project.post.controller")
			.build();
	}

	@Bean
	public GroupedOpenApi commentApi() {
		return GroupedOpenApi.builder()
			.group("comment")
			.packagesToScan("com.aivle.project.comment.controller")
			.build();
	}

	@Bean
	public GroupedOpenApi fileApi() {
		return GroupedOpenApi.builder()
			.group("file")
			.packagesToScan("com.aivle.project.file.controller")
			.build();
	}

	@Bean
	public GroupedOpenApi devApi() {
		return GroupedOpenApi.builder()
			.group("dev")
			.packagesToScan(
				"com.aivle.project.category.controller",
				"com.aivle.project.common.controller"
			)
			.build();
	}
}
