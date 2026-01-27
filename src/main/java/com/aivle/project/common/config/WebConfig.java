package com.aivle.project.common.config;

import com.aivle.project.common.security.CurrentUserArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;

/**
 * 웹 MVC 공통 설정.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final CurrentUserArgumentResolver currentUserArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
	}

	/**
	 * WebClient 빈 설정 (Turnstile API 호출용)
	 */
	@Bean
	public WebClient webClient() {
		return WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(
				HttpClient.create()
					.followRedirect(true)
					.compress(true)
			))
			.build();
	}
}
