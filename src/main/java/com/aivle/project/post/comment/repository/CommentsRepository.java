package com.aivle.project.post.comment.repository;

import com.aivle.project.post.comment.entity.CommentsEntity;
import com.aivle.project.auth.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CommentsRepository extends JpaRepository<CommentsEntity, Long> {
    // 소프트 삭제된 댓글을 제외하고 계층/순서대로 조회
    List<CommentsEntity> findByPostIdAndDeletedAtIsNullOrderByDepthAscSequenceAsc(Long postId);

    List<CommentsEntity> findByPostIdOrderByDepthAscSequenceAsc(Long postId);

	@Query("SELECT COALESCE(MAX(c.sequence), -1) FROM CommentsEntity c WHERE c.post.id = :postId AND c.parent IS NULL")
	int findMaxSequenceByPostIdAndParentIsNull(@Param("postId") Long postId);

	@Query("SELECT COALESCE(MAX(c.sequence), -1) FROM CommentsEntity c WHERE c.parent.id = :parentId")
	int findMaxSequenceByParentId(@Param("parentId") Long parentId);

	@Query("""
		select distinct c.post.id
		from CommentsEntity c
		where c.post.id in :postIds
			and c.deletedAt is null
			and exists (
				select 1
				from UserRoleEntity ur
				where ur.user = c.user
					and ur.role.name = :roleName
			)
		""")
	Set<Long> findAnsweredPostIdsByPostIdsAndRole(
		@Param("postIds") List<Long> postIds,
		@Param("roleName") RoleName roleName
	);

	@Query("""
		select count(c)
		from CommentsEntity c
		where c.post.id = :postId
			and c.deletedAt is null
			and exists (
				select 1
				from UserRoleEntity ur
				where ur.user = c.user
					and ur.role.name = :roleName
			)
		""")
	long countByPostIdAndRole(
		@Param("postId") Long postId,
		@Param("roleName") RoleName roleName
	);
}
