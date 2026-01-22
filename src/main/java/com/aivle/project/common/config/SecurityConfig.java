package com.aivle.project.common.config;

import com.aivle.project.auth.service.AccessTokenBlacklistService;
import com.aivle.project.auth.token.AccessTokenValidator;
import com.aivle.project.auth.token.JwtKeyProvider;
import com.aivle.project.auth.token.JwtProperties;
import com.aivle.project.common.security.RestAccessDeniedHandler;
import com.aivle.project.common.security.RestAuthenticationEntryPoint;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;

import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 공통 보안 설정.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtKeyProvider jwtKeyProvider;
	private final JwtProperties jwtProperties;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;

	@Bean
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
		throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.headers(headers -> headers
				.contentSecurityPolicy(csp -> csp
					.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; frame-ancestors 'none';")
				)
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/auth/login", "/auth/refresh", "/auth/signup", "/auth/console", "/error").permitAll()
				.requestMatchers("/assets/**").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// 실 운영 환경에서는 특정 도메인만 허용하도록 변경해야 합니다.
		configuration.setAllowedOriginPatterns(List.of("*"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtDecoder jwtDecoder(OAuth2TokenValidator<Jwt> accessTokenValidator) {
		RSAPublicKey publicKey = jwtKeyProvider.loadPublicKey();
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
		OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, accessTokenValidator));
		return decoder;
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		RSAPublicKey publicKey = jwtKeyProvider.loadPublicKey();
		RSAPrivateKey privateKey = jwtKeyProvider.loadPrivateKey();
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
			.privateKey(privateKey)
			.keyID(jwtProperties.getKeys().getCurrentKid())
			.build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
	}

	@Bean
	public OAuth2TokenValidator<Jwt> accessTokenValidator(AccessTokenBlacklistService accessTokenBlacklistService) {
		return new AccessTokenValidator(accessTokenBlacklistService);
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			List<String> roles = jwt.getClaimAsStringList("roles");
			if (roles == null || roles.isEmpty()) {
				return List.of();
			}

			boolean allowLegacyPrefix = isLegacyRolePrefixAllowed();
			if (!allowLegacyPrefix && containsLegacyPrefix(roles)) {
				throw new IllegalArgumentException("레거시 ROLE_ 접두사 토큰은 더 이상 허용되지 않습니다.");
			}

			List<GrantedAuthority> authorities = new ArrayList<>();
			for (String role : roles) {
				String normalized = normalizeRole(role, allowLegacyPrefix);
				if (normalized != null) {
					authorities.add(new SimpleGrantedAuthority(normalized));
				}
			}
			return authorities;
		});
		return converter;
	}

	private boolean isLegacyRolePrefixAllowed() {
		if (!jwtProperties.getLegacy().isRolePrefixSupportEnabled()) {
			return false;
		}
		long cutoffEpoch = jwtProperties.getLegacy().getRolePrefixAcceptUntilEpoch();
		if (cutoffEpoch <= 0) {
			return true;
		}
		return Instant.now().getEpochSecond() <= cutoffEpoch;
	}

	private String normalizeRole(String role, boolean allowLegacyPrefix) {
		if (role == null || role.isBlank()) {
			return null;
		}
		String trimmed = role.trim();
		if (trimmed.startsWith("ROLE_")) {
			return allowLegacyPrefix ? trimmed : null;
		}
		return "ROLE_" + trimmed;
	}

	private boolean containsLegacyPrefix(List<String> roles) {
		for (String role : roles) {
			if (role != null && role.trim().startsWith("ROLE_")) {
				return true;
			}
		}
		return false;
	}
}
