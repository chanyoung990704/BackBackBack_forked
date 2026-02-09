package com.aivle.project.file.service;

import com.aivle.project.common.error.CommonErrorCode;
import com.aivle.project.common.error.CommonException;
import com.aivle.project.file.dto.FileResponse;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.entity.PostFilesEntity;
import com.aivle.project.file.exception.FileErrorCode;
import com.aivle.project.file.exception.FileException;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.repository.PostFilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import com.aivle.project.file.validator.FileValidator;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.user.entity.UserEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 처리 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

	private final FileStorageService fileStorageService;
	private final FileValidator fileValidator;
	private final FilesRepository filesRepository;
	private final PostFilesRepository postFilesRepository;
	private final PostsRepository postsRepository;
	private final com.aivle.project.file.mapper.FileMapper fileMapper;

	public List<FileResponse> upload(Long postId, UserEntity user, List<MultipartFile> files) {
		PostsEntity post = findPost(postId);
		Long userId = requireUserId(user);
		validateOwner(post, userId);
		fileValidator.validateMultiple(files);

		String keyPrefix = "posts/" + postId;
		List<FileResponse> responses = new ArrayList<>();

		for (MultipartFile file : files) {
			StoredFile stored = fileStorageService.store(file, keyPrefix);
			FilesEntity entity = FilesEntity.create(
				FileUsageType.POST_ATTACHMENT,
				stored.storageUrl(),
				stored.storageKey(),
				stored.originalFilename(),
				stored.fileSize(),
				stored.contentType()
			);
			FilesEntity saved = filesRepository.save(entity);
			postFilesRepository.save(PostFilesEntity.create(post, saved));
			responses.add(fileMapper.toResponse(post.getId(), saved));
		}

		return responses;
	}

	@Transactional(readOnly = true)
	public List<FileResponse> list(Long postId, UserEntity user) {
		PostsEntity post = findPost(postId);
		requireUserId(user);
		return postFilesRepository.findAllActiveByPostIdOrderByCreatedAtAsc(postId).stream()
			.map(mapping -> fileMapper.toResponse(postId, mapping.getFile()))
			.toList();
	}

	@Transactional(readOnly = true)
	public FilesEntity getFile(Long fileId, UserEntity user) {
		PostFilesEntity mapping = postFilesRepository.findByFileId(fileId)
			.orElseThrow(() -> new FileException(FileErrorCode.FILE_404_NOT_FOUND));
		FilesEntity file = mapping.getFile();
		if (file.isDeleted()) {
			throw new FileException(FileErrorCode.FILE_404_NOT_FOUND);
		}
		requireUserId(user);
		return file;
	}

	private PostsEntity findPost(Long postId) {
		return postsRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new CommonException(CommonErrorCode.COMMON_404));
	}

	private void validateOwner(PostsEntity post, Long userId) {
		if (!post.getUser().getId().equals(userId)) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
	}

	private Long requireUserId(UserEntity user) {
		if (user == null || user.getId() == null) {
			throw new CommonException(CommonErrorCode.COMMON_403);
		}
		return user.getId();
	}
}
