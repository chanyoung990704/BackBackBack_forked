package com.aivle.project.file.storage;

import com.aivle.project.file.entity.FilesEntity;
import java.util.Optional;

/**
 * 파일 다운로드 URL 해석기.
 */
public interface FileDownloadUrlResolver {

	Optional<String> resolve(FilesEntity file);
}
