package com.aivle.project.auth.user.repository;

import com.aivle.project.auth.user.entity.RoleEntity;
import com.aivle.project.auth.user.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 역할 조회용 리포지토리.
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

	Optional<RoleEntity> findByName(RoleName name);
}
