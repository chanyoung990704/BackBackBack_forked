package com.aivle.project.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.user.entity.RoleEntity;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserRoleEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.RoleRepository;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.aivle.project.common.config.TestSecurityConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetails 로딩 및 탈퇴 흐름 통합 테스트.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestSecurityConfig.class)
class CustomUserDetailsServiceTest {

	@Autowired
	private CustomUserDetailsService customUserDetailsService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private RoleRepository roleRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("이메일로 조회 시 역할 권한이 UserDetails에 반영된다")
	void loadUserByUsername_returnsAuthoritiesFromRoles() {
		String email = "user@test.com";
		UserEntity user = createUser(email, UserStatus.ACTIVE);
		userRepository.save(user);

		RoleEntity role = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();

		userRoleRepository.save(new UserRoleEntity(user, role));
		entityManager.flush();
		entityManager.clear();

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

		assertThat(userDetails.getUsername()).isEqualTo(email);
		assertThat(userDetails.isEnabled()).isTrue();
		assertThat(userDetails.getAuthorities())
			.extracting(GrantedAuthority::getAuthority)
			.containsExactly("ROLE_USER");
	}

	@Test
	@DisplayName("역할이 없으면 빈 권한 목록을 반환한다")
	void loadUserByUsername_withoutRoles_returnsEmptyAuthorities() {
		String email = "norole@test.com";
		UserEntity user = createUser(email, UserStatus.ACTIVE);
		userRepository.save(user);
		entityManager.flush();
		entityManager.clear();

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

		assertThat(userDetails.getAuthorities()).isEmpty();
	}

	@Test
	@DisplayName("탈퇴 처리 시 상태가 INACTIVE로 바뀌고 삭제 시간이 기록된다")
	void withdraw_setsInactiveAndDeletedAt() {
		UserEntity user = createUser("withdraw@test.com", UserStatus.ACTIVE);

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
		assertThat(user.isDeleted()).isTrue();
		assertThat(user.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("이미 탈퇴된 사용자는 withdraw 재호출 시 상태/삭제 시간이 유지된다")
	void withdraw_doesNotChangeWhenAlreadyDeleted() {
		UserEntity user = createUser("withdraw-again@test.com", UserStatus.ACTIVE);

		user.withdraw();
		var firstDeletedAt = user.getDeletedAt();

		user.withdraw();

		assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
		assertThat(user.getDeletedAt()).isEqualTo(firstDeletedAt);
	}

	@Test
	@DisplayName("사용자 저장 시 UUID가 자동 생성된다")
	void save_setsUuid() {
		UserEntity user = createUser("uuid@test.com", UserStatus.ACTIVE);

		userRepository.save(user);
		entityManager.flush();

		assertThat(user.getUuid()).isNotNull();
	}

	private UserEntity createUser(String email, UserStatus status) {
		UserEntity user = newUserEntity();
		ReflectionTestUtils.setField(user, "email", email);
		ReflectionTestUtils.setField(user, "password", "encoded-password");
		ReflectionTestUtils.setField(user, "name", "test-user");
		ReflectionTestUtils.setField(user, "status", status);
		return user;
	}

	private UserEntity newUserEntity() {
		try {
			var ctor = UserEntity.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to create UserEntity", e);
		}
	}
}
