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
			UserStatus.PENDING  // 이메일 인증 대기 상태
		);
		userRepository.save(user);

		RoleEntity role = roleRepository.findByName(roleName)
			.orElseThrow(() -> new IllegalStateException("역할 시드가 존재하지 않습니다: " + roleName));
		userRoleRepository.save(new UserRoleEntity(user, role));

		return user;
	}

	/**
	 * 사용자 활성화 (이메일 인증 완료 시).
	 */
	@Transactional
	public void activateUser(Long userId) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
	}

	/**
	 * 사용자 조회.
	 */
	public UserEntity getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
	}

	/**
	 * 비밀번호 변경.
	 */
	@Transactional
	public void updatePassword(Long userId, String encodedPassword) {
		UserEntity user = getUserById(userId);
		user.updatePassword(encodedPassword);
	}
}
