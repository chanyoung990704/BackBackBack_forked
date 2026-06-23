package com.aivle.project.auth.user.security;

import com.aivle.project.auth.user.entity.RoleName;
import com.aivle.project.auth.user.entity.UserEntity;
import com.aivle.project.auth.user.entity.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * UserEntity 기반 UserDetails 어댑터.
 */
public class CustomUserDetails implements UserDetails {

	private final Long id;
	private final UUID uuid;
	private final String email;
	private final String password;
	private final String name;
	private final UserStatus status;
	private final boolean passwordExpired;
	private final List<GrantedAuthority> authorities;

	private CustomUserDetails(
		Long id,
		UUID uuid,
		String email,
		String password,
		String name,
		UserStatus status,
		boolean passwordExpired,
		List<GrantedAuthority> authorities
	) {
		this.id = id;
		this.uuid = uuid;
		this.email = email;
		this.password = password;
		this.name = name;
		this.status = status;
		this.passwordExpired = passwordExpired;
		this.authorities = authorities;
	}

	public static CustomUserDetails from(UserEntity user, Collection<RoleName> roles) {
		List<GrantedAuthority> authorities = roles.stream()
			.distinct()
			.<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.name()))
			.toList();

		return new CustomUserDetails(
			user.getId(),
			user.getUuid(),
			user.getEmail(),
			user.getPassword(),
			user.getName(),
			user.getStatus(),
			user.isPasswordExpired(),
			authorities
		);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public UUID getUuid() {
		return uuid;
	}

	public UserStatus getStatus() {
		return status;
	}

	public boolean isPasswordExpired() {
		return passwordExpired;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != UserStatus.BANNED;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return status == UserStatus.ACTIVE;
	}
}
