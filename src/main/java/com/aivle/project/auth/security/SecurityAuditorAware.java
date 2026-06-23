package com.aivle.project.auth.security;

import com.aivle.project.auth.user.security.CustomUserDetails;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Spring Security 기반 AuditorAware 구현체.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<Long> {

	@Override
	public Optional<Long> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return Optional.ofNullable(extractUserId(jwt));
		}

		if (principal instanceof CustomUserDetails customUserDetails) {
			return Optional.ofNullable(customUserDetails.getId());
		}

		if (principal instanceof UserDetails userDetails && userDetails instanceof CustomUserDetails customUserDetails) {
			return Optional.ofNullable(customUserDetails.getId());
		}

		if (principal instanceof String principalName && "anonymousUser".equals(principalName)) {
			return Optional.empty();
		}

		return Optional.empty();
	}

	private Long extractUserId(Jwt jwt) {
		Object claim = jwt.getClaim("userId");
		if (claim == null) {
			return null;
		}
		if (claim instanceof Number number) {
			return number.longValue();
		}
		if (claim instanceof String value) {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}
}
