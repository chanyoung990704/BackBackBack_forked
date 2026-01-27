package com.aivle.project.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.SignupRequest;
import com.aivle.project.auth.dto.SignupResponse;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.auth.service.TurnstileService;
import com.aivle.project.common.dto.ApiResponse;
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
		TokenResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
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
		ApiResponse<Void> response = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(response.error().code()).isEqualTo("AUTH_401");
	}

	@Test
	@DisplayName("유효한 Turnstile 토큰으로 회원가입 성공 시 사용자 생성 및 응답 반환")
	void signup_withValidTurnstileToken_shouldCreateUserAndReturnResponse() throws Exception {
		// given: 유효한 회원가입 요청 준비
		SignupRequest request = new SignupRequest();
		request.setEmail("newuser@test.com");
		request.setPassword("ValidPass123!");
		request.setName("테스트 사용자");
		request.setPhone("01012345678");
		request.setTurnstileToken("valid-turnstile-token");

		// when: 회원가입 요청 수행
		MvcResult result = mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// then: 회원가입 성공 응답 반환
		SignupResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), SignupResponse.class);
		assertThat(response.email()).isEqualTo("newuser@test.com");

		// 데이터베이스에 사용자가 생성되었는지 확인
		UserEntity createdUser = userRepository.findByEmail("newuser@test.com").orElse(null);
		assertThat(createdUser).isNotNull();
		assertThat(createdUser.getEmail()).isEqualTo("newuser@test.com");
		assertThat(createdUser.getName()).isEqualTo("테스트 사용자");
	}

	// Note: Turnstile 검증 실패 테스트는 TestSecurityConfig에서 항상 통과하도록 설정되어 있으므로
	// 실제 운영 환경에서 별도 테스트 환경을 구성해야 함

	@Test
	@DisplayName("Turnstile 토큰 누락 시 검증 실패 및 400 응답 반환")
	void signup_withoutTurnstileToken_shouldReturnBadRequest() throws Exception {
		// given: Turnstile 토큰 없는 요청
		SignupRequest request = new SignupRequest();
		request.setEmail("notoken@test.com");
		request.setPassword("ValidPass123!");
		request.setName("토큰 없음");
		request.setPhone("01012345678");
		// turnstileToken은 null로 유지

		// when: 회원가입 요청 수행
		MvcResult result = mockMvc.perform(post("/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andReturn();

		// then: 검증 실패 응답
		ApiResponse<Void> response = objectMapper.readValue(
			result.getResponse().getContentAsString(),
			new TypeReference<ApiResponse<Void>>() {}
		);
		assertThat(response.success()).isFalse();
		assertThat(response.error()).isNotNull();
	}

	private String loginAndGetAccessToken(String email, String password, String deviceId) throws Exception {
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

		TokenResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
		return response.accessToken();
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
