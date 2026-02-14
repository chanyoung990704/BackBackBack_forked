package com.aivle.project.file.repository;

import com.aivle.project.file.entity.PostFilesEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 게시글-파일 매핑 저장소.
 */
public interface PostFilesRepository extends JpaRepository<PostFilesEntity, Long> {

	@Query("select pf from PostFilesEntity pf join fetch pf.file f where pf.post.id = :postId and f.deletedAt is null order by pf.createdAt asc")
	List<PostFilesEntity> findAllActiveByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);

	@Query("select pf from PostFilesEntity pf join fetch pf.file f join fetch pf.post p where f.id = :fileId and f.deletedAt is null and p.deletedAt is null")
	Optional<PostFilesEntity> findByFileId(@Param("fileId") Long fileId);
}
