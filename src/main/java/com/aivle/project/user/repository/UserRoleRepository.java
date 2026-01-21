package com.aivle.project.user.repository;

import com.aivle.project.user.entity.UserRoleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 사용자-역할 매핑 리포지토리.
 */
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

	List<UserRoleEntity> findAllByUserId(Long userId);

	@Query("select ur from UserRoleEntity ur join fetch ur.user u join fetch ur.role r where u.email = :email")
	List<UserRoleEntity> findAllWithUserAndRoleByUserEmail(@Param("email") String email);
}
