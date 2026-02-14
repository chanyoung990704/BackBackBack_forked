package com.aivle.project.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

import com.aivle.project.common.error.CommonException;
import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.file.dto.FileResponse;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import com.aivle.project.file.entity.PostFilesEntity;
import com.aivle.project.file.repository.FilesRepository;
import com.aivle.project.file.repository.PostFilesRepository;
import com.aivle.project.file.storage.FileStorageService;
import com.aivle.project.file.storage.StoredFile;
import com.aivle.project.file.validator.FileValidator;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.post.service.PostReadAccessPolicy;
import com.aivle.project.user.entity.UserEntity;
import com.aivle.project.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

	@InjectMocks
	private FileService fileService;

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private FileValidator fileValidator;

	@Mock
	private FilesRepository filesRepository;

	@Mock
	private PostFilesRepository postFilesRepository;

	@Mock
	private PostsRepository postsRepository;

	@Spy
	private PostReadAccessPolicy postReadAccessPolicy;

	@Mock
	private com.aivle.project.file.mapper.FileMapper fileMapper;

	@Test
	@DisplayName("게시글 파일을 업로드하면 메타데이터가 저장된다")
	void upload_shouldStoreFiles() {
		// given
		Long postId = 10L;
		UserEntity user = newUser(1L, UUID.randomUUID());
		PostsEntity post = newPost(postId, user);
		List<MultipartFile> files = List.of(
			new MockMultipartFile("files", "a.png", "image/png", new byte[] {1, 2, 3}),
			new MockMultipartFile("files", "b.png", "image/png", new byte[] {4, 5, 6})
		);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));
		doNothing().when(fileValidator).validateMultiple(files);

		AtomicLong sequence = new AtomicLong(1);
		given(fileStorageService.store(any(MultipartFile.class), eq("posts/" + postId)))
			.willAnswer(invocation -> {
				long index = sequence.get();
				return new StoredFile(
					"url-" + index,
					"file-" + index + ".png",
					10L,
					"image/png",
					"key-" + index
				);
			});

		given(filesRepository.save(any(FilesEntity.class))).willAnswer(invocation -> {
			FilesEntity entity = invocation.getArgument(0);
			ReflectionTestUtils.setField(entity, "id", sequence.getAndIncrement());
			return entity;
		});
		given(postFilesRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		given(fileMapper.toResponse(eq(postId), any(FilesEntity.class))).willAnswer(invocation -> {
			FilesEntity entity = invocation.getArgument(1);
			return new FileResponse(entity.getId(), postId, entity.getStorageUrl(), entity.getOriginalFilename(), entity.getFileSize(), entity.getContentType(), null);
		});

		// when
		List<FileResponse> responses = fileService.upload(postId, user, files);

		// then
		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).postId()).isEqualTo(postId);
		assertThat(responses.get(0).storageUrl()).isEqualTo("url-1");
		assertThat(responses.get(1).storageUrl()).isEqualTo("url-2");
		verify(fileValidator).validateMultiple(files);
		verify(fileStorageService, times(2)).store(any(MultipartFile.class), eq("posts/" + postId));
	}

	@Test
	@DisplayName("작성자가 아니면 업로드 요청이 실패한다")
	void upload_shouldFailWhenNotOwner() {
		// given
		Long postId = 10L;
		UserEntity owner = newUser(1L, UUID.randomUUID());
		UserEntity other = newUser(2L, UUID.randomUUID());
		PostsEntity post = newPost(postId, owner);
		List<MultipartFile> files = List.of(
			new MockMultipartFile("files", "a.png", "image/png", new byte[] {1})
		);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));

		// when & then
		assertThatThrownBy(() -> fileService.upload(postId, other, files))
			.isInstanceOf(CommonException.class);
		verifyNoInteractions(fileValidator, fileStorageService, filesRepository, postFilesRepository);
	}

	@Test
	@DisplayName("게시글이 없으면 업로드 요청이 실패한다")
	void upload_shouldFailWhenPostMissing() {
		// given
		Long postId = 10L;
		UserEntity user = newUser(1L, UUID.randomUUID());
		List<MultipartFile> files = List.of(
			new MockMultipartFile("files", "a.png", "image/png", new byte[] {1})
		);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> fileService.upload(postId, user, files))
			.isInstanceOf(CommonException.class);
		verifyNoInteractions(fileValidator, fileStorageService, filesRepository, postFilesRepository);
	}

	@Test
	@DisplayName("로그인 사용자라면 작성자가 아니어도 파일 다운로드가 가능하다")
	void getFile_shouldAllowNonOwnerUser() {
		// given
		Long postId = 10L;
		Long fileId = 5L;
		UserEntity owner = newUser(1L, UUID.randomUUID());
		UserEntity other = newUser(2L, UUID.randomUUID());
		PostsEntity post = newPost(postId, owner);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"url",
			"key",
			"file.png",
			10L,
			"image/png"
		);
		ReflectionTestUtils.setField(file, "id", fileId);
		PostFilesEntity mapping = PostFilesEntity.create(post, file);

		given(postFilesRepository.findByFileId(fileId)).willReturn(Optional.of(mapping));

		// when
		FilesEntity result = fileService.getFile(fileId, other);

		// then
		assertThat(result).isSameAs(file);
	}

	@Test
	@DisplayName("QnA 게시글 파일은 작성자가 아니면 다운로드할 수 없다")
	void getFile_shouldFailWhenQnaNotOwner() {
		// given
		Long postId = 10L;
		Long fileId = 5L;
		UserEntity owner = newUser(1L, UUID.randomUUID());
		UserEntity other = newUser(2L, UUID.randomUUID());
		PostsEntity post = newPost(postId, owner);
		setCategory(post, "qna");
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"url",
			"key",
			"file.png",
			10L,
			"image/png"
		);
		ReflectionTestUtils.setField(file, "id", fileId);
		PostFilesEntity mapping = PostFilesEntity.create(post, file);
		given(postFilesRepository.findByFileId(fileId)).willReturn(Optional.of(mapping));

		// when & then
		assertThatThrownBy(() -> fileService.getFile(fileId, other))
			.isInstanceOf(CommonException.class);
	}

	@Test
	@DisplayName("로그인하지 않은 사용자는 파일 다운로드가 불가능하다")
	void getFile_shouldFailWhenUserMissing() {
		// given
		Long postId = 10L;
		Long fileId = 5L;
		UserEntity owner = newUser(1L, UUID.randomUUID());
		PostsEntity post = newPost(postId, owner);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"url",
			"key",
			"file.png",
			10L,
			"image/png"
		);
		ReflectionTestUtils.setField(file, "id", fileId);
		PostFilesEntity mapping = PostFilesEntity.create(post, file);

		given(postFilesRepository.findByFileId(fileId)).willReturn(Optional.of(mapping));

		// when & then
		assertThatThrownBy(() -> fileService.getFile(fileId, null))
			.isInstanceOf(CommonException.class);
	}

	@Test
	@DisplayName("로그인 사용자라면 작성자가 아니어도 파일 목록을 조회할 수 있다")
	void list_shouldAllowNonOwnerUser() {
		// given
		Long postId = 10L;
		UserEntity owner = newUser(1L, UUID.randomUUID());
		UserEntity other = newUser(2L, UUID.randomUUID());
		PostsEntity post = newPost(postId, owner);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"url",
			"key",
			"file.png",
			10L,
			"image/png"
		);
		ReflectionTestUtils.setField(file, "id", 99L);
		PostFilesEntity mapping = PostFilesEntity.create(post, file);

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));
		given(postFilesRepository.findAllActiveByPostIdOrderByCreatedAtAsc(postId))
			.willReturn(List.of(mapping));
		given(fileMapper.toResponse(eq(postId), any(FilesEntity.class)))
			.willReturn(new FileResponse(file.getId(), postId, file.getStorageUrl(), file.getOriginalFilename(),
				file.getFileSize(), file.getContentType(), null));

		// when
		List<FileResponse> responses = fileService.list(postId, other);

		// then
		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).postId()).isEqualTo(postId);
	}

	@Test
	@DisplayName("QnA 게시글 파일 목록은 작성자가 아니면 조회할 수 없다")
	void list_shouldFailWhenQnaNotOwner() {
		// given
		Long postId = 10L;
		UserEntity owner = newUser(1L, UUID.randomUUID());
		UserEntity other = newUser(2L, UUID.randomUUID());
		PostsEntity post = newPost(postId, owner);
		setCategory(post, "qna");

		given(postsRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.of(post));

		// when & then
		assertThatThrownBy(() -> fileService.list(postId, other))
			.isInstanceOf(CommonException.class);
	}

	private UserEntity newUser(Long id, UUID uuid) {
		try {
			var ctor = UserEntity.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			UserEntity user = ctor.newInstance();
			ReflectionTestUtils.setField(user, "id", id);
			ReflectionTestUtils.setField(user, "uuid", uuid);
			ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
			return user;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("UserEntity 생성에 실패했습니다", ex);
		}
	}

	private PostsEntity newPost(Long id, UserEntity user) {
		try {
			var ctor = PostsEntity.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			PostsEntity post = ctor.newInstance();
			ReflectionTestUtils.setField(post, "id", id);
			ReflectionTestUtils.setField(post, "user", user);
			return post;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("PostsEntity 생성에 실패했습니다", ex);
		}
	}

	private void setCategory(PostsEntity post, String name) {
		try {
			var ctor = CategoriesEntity.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			CategoriesEntity category = ctor.newInstance();
			ReflectionTestUtils.setField(category, "name", name);
			ReflectionTestUtils.setField(post, "category", category);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("CategoriesEntity 생성에 실패했습니다", ex);
		}
	}
}
