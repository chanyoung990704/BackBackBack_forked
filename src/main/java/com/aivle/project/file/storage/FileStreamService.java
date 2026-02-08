package com.aivle.project.file.storage;

import com.aivle.project.file.entity.FilesEntity;
import java.io.InputStream;

/**
 * 파일 스트리밍 서비스.
 */
public interface FileStreamService {

	InputStream openStream(FilesEntity file);
}
