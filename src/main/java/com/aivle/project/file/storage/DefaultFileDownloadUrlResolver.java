package com.aivle.project.file.storage;

import com.aivle.project.file.entity.FilesEntity;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 기본 다운로드 URL 해석기 (prod 제외).
 */
@Component
@Profile("!prod")
public class DefaultFileDownloadUrlResolver implements FileDownloadUrlResolver {

	@Override
	public Optional<String> resolve(FilesEntity file) {
		return Optional.empty();
	}
}
