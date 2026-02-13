package com.aivle.project.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.aivle.project.common.config.QuerydslConfig;
import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.entity.RoleEntity;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserRoleEntity;
import com.aivle.project.user.entity.UserStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(QuerydslConfig.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@AfterEach
	void tearDown() {
		userRoleRepository.deleteAll();
		userRepository.deleteAll();
		roleRepository.deleteAll();
	}

	@Test
	@DisplayName("활성 사용자 목록 조회 시 ROLE_ADMIN 보유 사용자를 제외한다")
	void findActiveUsersExcludingRole_shouldExcludeAdminUsers() {
		// given
		UserEntity normalUser = userRepository.save(
			UserEntity.create("user@test.com", "encoded", "일반사용자", "010-1111-1111", UserStatus.ACTIVE)
		);
		UserEntity adminUser = userRepository.save(
			UserEntity.create("admin@test.com", "encoded", "관리자", "010-2222-2222", UserStatus.ACTIVE)
		);
		UserEntity mixedRoleUser = userRepository.save(
			UserEntity.create("mixed@test.com", "encoded", "복합권한", "010-3333-3333", UserStatus.ACTIVE)
		);

		RoleEntity userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
		RoleEntity adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

		userRoleRepository.save(new UserRoleEntity(normalUser, userRole));
		userRoleRepository.save(new UserRoleEntity(adminUser, adminRole));
		userRoleRepository.save(new UserRoleEntity(mixedRoleUser, userRole));
		userRoleRepository.save(new UserRoleEntity(mixedRoleUser, adminRole));

		// when
		List<AdminUserListItemDto> result = userRepository.findListByStatusAndDeletedAtIsNullOrderByIdAscExcludingRole(
			UserStatus.ACTIVE,
			RoleName.ROLE_ADMIN
		);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).email()).isEqualTo("user@test.com");
	}
}
