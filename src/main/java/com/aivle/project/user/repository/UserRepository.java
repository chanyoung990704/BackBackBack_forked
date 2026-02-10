package com.aivle.project.user.repository;

import com.aivle.project.user.dto.AdminUserListItemDto;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 사용자 조회용 리포지토리.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByEmail(String email);

	Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

	Optional<UserEntity> findByUuidAndDeletedAtIsNull(UUID uuid);

	@Query("""
		select new com.aivle.project.user.dto.AdminUserListItemDto(u.id, u.name, u.email)
		from UserEntity u
		where u.status = :status
			and u.deletedAt is null
		order by u.id asc
		""")
	List<AdminUserListItemDto> findListByStatusAndDeletedAtIsNullOrderByIdAsc(@Param("status") UserStatus status);

	boolean existsByIdAndStatusAndDeletedAtIsNull(Long id, UserStatus status);
}
