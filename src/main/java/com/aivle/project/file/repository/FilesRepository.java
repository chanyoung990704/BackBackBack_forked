package com.aivle.project.file.repository;

import com.aivle.project.file.entity.FilesEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 파일 저장소.
 */
public interface FilesRepository extends JpaRepository<FilesEntity, Long> {

	List<FilesEntity> findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long postId);
}
