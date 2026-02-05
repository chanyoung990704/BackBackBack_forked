package com.aivle.project.user.entity;

import com.aivle.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * users 테이블에 매핑되는 사용자 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "uuid", nullable = false, columnDefinition = "BINARY(16)")
	private UUID uuid;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "password_changed_at")
	private java.time.LocalDateTime passwordChangedAt;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "phone", length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.PENDING;

	@PrePersist
	private void prePersist() {
		if (uuid == null) {
			uuid = UUID.randomUUID();
		}
		if (status == null) {
			status = UserStatus.PENDING;
		}
		if (passwordChangedAt == null) {
			passwordChangedAt = java.time.LocalDateTime.now();
		}
	}

	/**
	 * 회원가입용 사용자 생성.
	 */
	public static UserEntity create(String email, String encodedPassword, String name, String phone, UserStatus status) {
		UserEntity user = new UserEntity();
		user.email = email;
		user.password = encodedPassword;
		user.name = name;
		user.phone = phone;
		user.status = status;
		user.passwordChangedAt = java.time.LocalDateTime.now();
		return user;
	}

	public void withdraw() {
		if (isDeleted()) {
			return;
		}
		status = UserStatus.INACTIVE;
		delete();
	}

	/**
	 * 사용자 상태 변경.
	 */
	public void setStatus(UserStatus status) {
		this.status = status;
	}

	/**
	 * 비밀번호 변경.
	 */
	public void updatePassword(String encodedPassword) {
		this.password = encodedPassword;
		this.passwordChangedAt = java.time.LocalDateTime.now();
	}

	/**
	 * 비밀번호 만료 여부 확인 (90일).
	 */
	public boolean isPasswordExpired() {
		java.time.LocalDateTime lastChanged = passwordChangedAt != null ? passwordChangedAt : getCreatedAt();
		if (lastChanged == null) {
			return false; // 생성일조차 없으면 아직 영속화되지 않은 상태
		}
		return lastChanged.isBefore(java.time.LocalDateTime.now().minusDays(90));
	}
}
