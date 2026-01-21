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

	@Column(name = "company_id")
	private Long companyId;

	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "uuid", nullable = false, columnDefinition = "BINARY(16)")
	private UUID uuid;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

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
	}

	public void withdraw() {
		if (isDeleted()) {
			return;
		}
		status = UserStatus.INACTIVE;
		delete();
	}
}
