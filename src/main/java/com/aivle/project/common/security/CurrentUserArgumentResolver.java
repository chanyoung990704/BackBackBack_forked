package com.aivle.project.common.security;

import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * JWT subject를 기반으로 현재 사용자 엔티티를 주입한다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final UserRepository userRepository;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class)
			&& UserEntity.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		Jwt jwt = extractJwt(authentication);
		if (jwt == null) {
			return null;
		}

		UUID userUuid = parseUserUuid(jwt.getSubject());
		return userRepository.findByUuidAndDeletedAtIsNull(userUuid)
			.orElse(null);
	}

	private Jwt extractJwt(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken token) {
			return token.getToken();
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt;
		}
		return null;
	}

	private UUID parseUserUuid(String subject) {
		if (subject == null || subject.isBlank()) {
			throw new CommonException(CommonErrorCode.COMMON_400);
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException ex) {
			throw new CommonException(CommonErrorCode.COMMON_400);
		}
	}
}
