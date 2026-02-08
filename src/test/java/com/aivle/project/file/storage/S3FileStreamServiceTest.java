package com.aivle.project.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.project.file.config.FileStorageProperties;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3FileStreamServiceTest {

	@Test
	@DisplayName("storageKey 기준으로 S3 스트림을 연다")
	void openStream_shouldUseStorageKey() {
		// given
		S3Client s3Client = mock(S3Client.class);
		FileStorageProperties properties = new FileStorageProperties();
		properties.getS3().setBucket("test-bucket");
		S3FileStreamService service = new S3FileStreamService(s3Client, properties);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/a.pdf",
			"uploads/a.pdf",
			"a.pdf",
			100L,
			"application/pdf"
		);

		ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
		when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

		// when
		InputStream result = service.openStream(file);

		// then
		assertThat(result).isSameAs(responseStream);
		ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(s3Client).getObject(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
		assertThat(captor.getValue().key()).isEqualTo("uploads/a.pdf");
	}

	@Test
	@DisplayName("storageKey가 없으면 storageUrl에서 키를 추출한다")
	void openStream_shouldResolveKeyFromUrl() {
		// given
		S3Client s3Client = mock(S3Client.class);
		FileStorageProperties properties = new FileStorageProperties();
		properties.getS3().setBucket("test-bucket");
		S3FileStreamService service = new S3FileStreamService(s3Client, properties);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/b.pdf",
			null,
			"b.pdf",
			100L,
			"application/pdf"
		);

		ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);
		when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

		// when
		service.openStream(file);

		// then
		ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
		verify(s3Client).getObject(captor.capture());
		assertThat(captor.getValue().key()).isEqualTo("uploads/b.pdf");
	}
}
