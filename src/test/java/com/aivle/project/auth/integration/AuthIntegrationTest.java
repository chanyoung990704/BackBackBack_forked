package com.aivle.project.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aivle.project.auth.dto.LoginRequest;
import com.aivle.project.auth.dto.TokenResponse;
import com.aivle.project.common.error.ErrorResponse;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
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
@Import(AuthIntegrationTest.TestSecurityController.class)
class AuthIntegrationTest {

	private static final String TEST_ISSUER = "project-test";
	private static final String TEST_KID = "test-kid";

	private static final Path PRIVATE_KEY_PATH;
	private static final Path PUBLIC_KEY_PATH;

	@Container
	static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
		.withExposedPorts(6379);

	static {
		try {
			Path tempDir = Files.createTempDirectory("jwt-keys");
			KeyPair keyPair = generateKeyPair();
			PRIVATE_KEY_PATH = tempDir.resolve("private.pem");
			PUBLIC_KEY_PATH = tempDir.resolve("public.pem");
			writePem(PRIVATE_KEY_PATH, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
			writePem(PUBLIC_KEY_PATH, "PUBLIC KEY", keyPair.getPublic().getEncoded());
		} catch (IOException ex) {
			throw new IllegalStateException("JWT 테스트 키 생성에 실패했습니다", ex);
		}
	}

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
		registry.add("jwt.keys.private-key-path", () -> PRIVATE_KEY_PATH.toString());
		registry.add("jwt.keys.public-key-path", () -> PUBLIC_KEY_PATH.toString());
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
		ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
		assertThat(response.code()).isEqualTo("AUTH_401");
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

	private static KeyPair generateKeyPair() throws IOException {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (Exception ex) {
			throw new IOException("RSA 키 생성에 실패했습니다", ex);
		}
	}

	private static void writePem(Path path, String type, byte[] encoded) throws IOException {
		String base64 = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded);
		String pem = "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
		Files.writeString(path, pem, StandardCharsets.US_ASCII);
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
