package com.aivle.project.user.service;

import com.aivle.project.user.entity.RoleEntity;
import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserRoleEntity;
import com.aivle.project.user.entity.UserStatus;
import com.aivle.project.user.repository.RoleRepository;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 도메인 로직을 담당하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;

	public boolean existsByEmail(String email) {
		return userRepository.findByEmail(email).isPresent();
	}

	@Transactional
	public UserEntity register(String email, String encodedPassword, String name, String phone, RoleName roleName) {
		UserEntity user = UserEntity.create(
			email,
			encodedPassword,
			name,
			phone,
			UserStatus.ACTIVE
		);
		userRepository.save(user);

		RoleEntity role = roleRepository.findByName(roleName)
			.orElseGet(() -> roleRepository.save(new RoleEntity(roleName, roleName.name().toLowerCase() + " role")));
		userRoleRepository.save(new UserRoleEntity(user, role));

		return user;
	}
}
