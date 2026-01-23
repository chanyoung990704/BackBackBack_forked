package com.aivle.project.file.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.post.entity.PostsEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * files 테이블에 매핑되는 파일 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "files")
public class FilesEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private PostsEntity post;

	@Column(name = "storage_url", nullable = false, length = 500)
	private String storageUrl;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_by")
	private Long updatedBy;

	public static FilesEntity create(
		PostsEntity post,
		String storageUrl,
		String originalFilename,
		long fileSize,
		String contentType,
		Long actorId
	) {
		FilesEntity file = new FilesEntity();
		file.post = post;
		file.storageUrl = storageUrl;
		file.originalFilename = originalFilename;
		file.fileSize = fileSize;
		file.contentType = contentType;
		file.createdBy = actorId;
		file.updatedBy = actorId;
		return file;
	}
}
