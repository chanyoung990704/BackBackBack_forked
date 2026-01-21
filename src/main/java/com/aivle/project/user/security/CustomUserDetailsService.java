package com.aivle.project.user.security;

import com.aivle.project.user.entity.RoleName;
import com.aivle.project.user.repository.UserRepository;
import com.aivle.project.user.repository.UserRoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자/권한 정보를 로드하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		var userRoles = userRoleRepository.findAllWithUserAndRoleByUserEmail(username);
		if (userRoles.isEmpty()) {
			var user = userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
			return CustomUserDetails.from(user, List.of());
		}

		var user = userRoles.get(0).getUser();
		List<RoleName> roles = userRoles.stream()
			.map(userRole -> userRole.getRole().getName())
			.toList();

		return CustomUserDetails.from(user, roles);
	}
}
