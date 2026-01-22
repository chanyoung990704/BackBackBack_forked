package com.aivle.project.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.repository.RefreshTokenRepository;
import com.aivle.project.common.dto.ApiResponse;
import com.aivle.project.common.error.ErrorResponse;
import com.aivle.project.common.config.TestSecurityConfig;
import com.aivle.project.user.entity.RoleEntity;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserRoleEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.repository.UserRoleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
@Import({AuthIntegrationTest.TestSecurityController.class, TestSecurityConfig.class})
class AuthIntegrationTest {

	private static final String TEST_ISSUER = "project-test";
	private static final String TEST_KID = "test-kid";

	@Container
	static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
		.withExposedPorts(6379);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@DynamicPropertySource
	static void registerJwtProperties(DynamicPropertyRegistry registry) {
		registry.add("jwt.issuer", () -> TEST_ISSUER);
		registry.add("jwt.access-token.expiration", () -> 1800);
		registry.add("jwt.refresh-token.expiration", () -> 604800);
		registry.add("jwt.keys.current-kid", () -> TEST_KID);
		registry.add("jwt.legacy.role-prefix-support-enabled", () -> true);
		registry.add("jwt.legacy.role-prefix-accept-until-epoch", () -> 0);
	}

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Test
	@DisplayName("로그인 성공 시 토큰 응답을 반환한다")
	void login_shouldReturnTokens() throws Exception {
		// given: 활성 사용자와 역할을 준비
		UserEntity user = createActiveUserWithRole("user@test.com", "password", RoleName.USER);

		LoginRequest request = new LoginRequest();
		request.setEmail("user@test.com");
		request.setPassword("password");
		request.setDeviceId("device-1");
		request.setDeviceInfo("ios");

		// when: 로그인 요청을 수행
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		// then: 토큰 응답이 반환된다
		ApiResponse<TokenResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<TokenResponse>>() {}
		);
		TokenResponse response = apiResponse.data();
		assertThat(apiResponse.success()).isTrue();
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");

		String refreshToken = response.refreshToken();
		String refreshKey = "refresh:" + refreshToken;
		String sessionKey = "sessions:" + user.getUuid();
		assertThat(redisTemplate.opsForValue().get(refreshKey)).isNotNull();
		assertThat(redisTemplate.opsForSet().isMember(sessionKey, refreshToken)).isTrue();
	}

	@Test
	@DisplayName("로그인 후 JWT 인증으로 보호된 엔드포인트에 접근한다")
	void login_shouldAuthenticateWithJwtClaimsAndAuthorities() throws Exception {
		// given: 활성 사용자와 역할을 준비
		createActiveUserWithRole("auth@test.com", "password", RoleName.USER);
		String accessToken = loginAndGetAccessToken("auth@test.com", "password", "device-1");

		// when: 보호된 엔드포인트에 접근
		MvcResult claimsResult = mockMvc.perform(get("/test/claims")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andReturn();

		MvcResult authoritiesResult = mockMvc.perform(get("/test/authorities")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andReturn();

		// then: 클레임과 권한이 검증된다
		JsonNode claims = objectMapper.readTree(claimsResult.getResponse().getContentAsString());
		assertThat(claims.get("email").asText()).isEqualTo("auth@test.com");
		assertThat(claims.get("deviceId").asText()).isEqualTo("device-1");
		assertThat(claims.get("issuer").asText()).isEqualTo(TEST_ISSUER);
		List<String> roles = objectMapper.convertValue(claims.get("roles"), new TypeReference<>() {});
		assertThat(roles).contains("USER");

		List<String> authorities = objectMapper.readValue(
			authoritiesResult.getResponse().getContentAsString(),
			objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
		);
		assertThat(authorities).contains("ROLE_USER");
	}

	@Test
	@DisplayName("인증 토큰 없이 접근하면 401 응답을 반환한다")
	void accessWithoutToken_shouldReturnUnauthorized() throws Exception {
		// when: 토큰 없이 보호된 엔드포인트 접근
		MvcResult result = mockMvc.perform(get("/test/claims"))
			.andExpect(status().isUnauthorized())
			.andReturn();

		// then: 표준 에러 응답을 반환한다
		ApiResponse<Void> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(apiResponse.success()).isFalse();
		assertThat(apiResponse.error().code()).isEqualTo("AUTH_401");
	}

	@Test
	@DisplayName("회원가입 성공 시 사용자 정보가 반환된다")
	void signup_shouldReturnCreatedUser() throws Exception {
		// given: 회원가입 요청을 준비
		SignupRequest request = new SignupRequest();
		request.setEmail("signup@test.com");
		request.setPassword("password123");
		request.setName("signup-user");
		request.setPhone("01012345678");

		// when: 회원가입 요청을 수행
		MvcResult result = mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// then: 응답과 DB 저장을 확인
		ApiResponse<SignupResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<SignupResponse>>() {}
		);
		SignupResponse response = apiResponse.data();
		assertThat(apiResponse.success()).isTrue();
		assertThat(response.email()).isEqualTo("signup@test.com");
		assertThat(response.role()).isEqualTo(RoleName.USER);
		assertThat(userRepository.findByEmail("signup@test.com")).isPresent();
	}

	@Test
	@DisplayName("로그아웃 시 Access Token이 무효화되고 Refresh Token이 폐기된다")
	void logout_shouldInvalidateAccessAndRefreshToken() throws Exception {
		// given: 활성 사용자와 로그인 토큰을 준비
		UserEntity user = createActiveUserWithRole("logout@test.com", "password", RoleName.USER);
		TokenResponse tokens = loginAndGetTokenResponse("logout@test.com", "password", "device-logout");

		String accessToken = tokens.accessToken();
		String refreshToken = tokens.refreshToken();
		String refreshKey = "refresh:" + refreshToken;
		String sessionKey = "sessions:" + user.getUuid();

		assertThat(redisTemplate.opsForValue().get(refreshKey)).isNotNull();
		assertThat(redisTemplate.opsForSet().isMember(sessionKey, refreshToken)).isTrue();

		// when: 로그아웃 요청을 수행
		mockMvc.perform(post("/auth/logout")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());

		// then: 기존 Access Token은 무효화되고 Refresh Token은 폐기된다
		mockMvc.perform(get("/test/claims")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isUnauthorized());

		assertThat(redisTemplate.opsForValue().get(refreshKey)).isNull();
		assertThat(redisTemplate.opsForSet().isMember(sessionKey, refreshToken)).isFalse();

		var entity = refreshTokenRepository.findByToken(refreshToken);
		assertThat(entity).isPresent();
		assertThat(entity.get().isRevoked()).isTrue();
	}

	@Test
	@DisplayName("전체 로그아웃 후 기존 토큰은 차단되고 새 로그인 토큰은 정상 동작한다")
	void logoutAll_shouldInvalidateExistingTokenAndAllowNewLogin() throws Exception {
		// given: 활성 사용자와 로그인 토큰을 준비
		createActiveUserWithRole("logoutall@test.com", "password", RoleName.USER);
		TokenResponse firstTokens = loginAndGetTokenResponse("logoutall@test.com", "password", "device-1");

		// when: 전체 로그아웃을 수행
		mockMvc.perform(post("/auth/logout-all")
				.header("Authorization", "Bearer " + firstTokens.accessToken()))
			.andExpect(status().isOk());

		// then: 기존 토큰은 차단된다
		mockMvc.perform(get("/test/claims")
				.header("Authorization", "Bearer " + firstTokens.accessToken()))
			.andExpect(status().isUnauthorized());

		// then: 새 로그인 토큰은 정상 동작한다
		TokenResponse secondTokens = loginAndGetTokenResponse("logoutall@test.com", "password", "device-2");
		mockMvc.perform(get("/test/claims")
				.header("Authorization", "Bearer " + secondTokens.accessToken()))
			.andExpect(status().isOk());
	}

	private String loginAndGetAccessToken(String email, String password, String deviceId) throws Exception {
		return loginAndGetTokenResponse(email, password, deviceId).accessToken();
	}

	private TokenResponse loginAndGetTokenResponse(String email, String password, String deviceId) throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail(email);
		request.setPassword(password);
		request.setDeviceId(deviceId);
		request.setDeviceInfo("test-device");

		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		ApiResponse<TokenResponse> apiResponse = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<TokenResponse>>() {}
		);
		return apiResponse.data();
	}

	private UserEntity createActiveUserWithRole(String email, String rawPassword, RoleName roleName) {
		UserEntity user = newUser(email, rawPassword);
		userRepository.save(user);

		RoleEntity role = new RoleEntity(roleName, roleName.name().toLowerCase() + " role");
		entityManager.persist(role);

		userRoleRepository.save(new UserRoleEntity(user, role));
		entityManager.flush();
		return user;
	}

	private UserEntity newUser(String email, String rawPassword) {
		try {
			var ctor = UserEntity.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			UserEntity user = ctor.newInstance();
			ReflectionTestUtils.setField(user, "email", email);
			ReflectionTestUtils.setField(user, "password", passwordEncoder.encode(rawPassword));
			ReflectionTestUtils.setField(user, "name", "test-user");
			ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
			return user;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("UserEntity 생성에 실패했습니다", ex);
		}
	}

	@RestController
	static class TestSecurityController {

		@GetMapping("/test/claims")
		public Map<String, Object> claims(@AuthenticationPrincipal Jwt jwt) {
			Map<String, Object> response = new HashMap<>();
			response.put("sub", jwt.getSubject());
			response.put("email", jwt.getClaimAsString("email"));
			response.put("deviceId", jwt.getClaimAsString("deviceId"));
			response.put("roles", jwt.getClaimAsStringList("roles"));
			response.put("issuer", jwt.getClaimAsString("iss"));
			return response;
		}

		@GetMapping("/test/authorities")
		public List<String> authorities(Authentication authentication) {
			return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
		}
	}
}
