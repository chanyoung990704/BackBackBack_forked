package com.aivle.project.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aivle.project.file.config.FileStorageProperties;
import com.aivle.project.file.entity.FileUsageType;
import com.aivle.project.file.entity.FilesEntity;
import java.net.URL;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3FileDownloadUrlResolverTest {

	@Test
	@DisplayName("storage_key가 있으면 presigned URL을 반환한다")
	void resolve_shouldReturnPresignedUrlWhenStorageKeyPresent() throws Exception {
		// given
		S3Presigner presigner = mock(S3Presigner.class);
		FileStorageProperties properties = new FileStorageProperties();
		properties.getS3().setBucket("bucket");

		PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
		when(presignedRequest.url()).thenReturn(new URL("https://example.com/presigned"));
		when(presigner.presignGetObject(org.mockito.ArgumentMatchers.<GetObjectPresignRequest>any()))
			.thenReturn(presignedRequest);

		S3FileDownloadUrlResolver resolver = new S3FileDownloadUrlResolver(presigner, properties);
		FilesEntity file = FilesEntity.create(
			FileUsageType.POST_ATTACHMENT,
			"https://bucket.s3.ap-northeast-2.amazonaws.com/uploads/a.pdf",
			"uploads/a.pdf",
			"a.pdf",
			100L,
			"application/pdf"
		);

		// when
		Optional<String> resolved = resolver.resolve(file);

		// then
		assertThat(resolved).contains("https://example.com/presigned");
	}
}
