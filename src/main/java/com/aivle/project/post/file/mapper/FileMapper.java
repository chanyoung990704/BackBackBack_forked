package com.aivle.project.post.file.mapper;

import com.aivle.project.file.dto.FileResponse;
import com.aivle.project.file.entity.FilesEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMapper {

	// 게시글 id는 엔티티가 아니라 호출 시 전달되는 값으로 채운다.
	@Mapping(target = "id", source = "entity.id")
	@Mapping(target = "postId", source = "postId")
	@Mapping(target = "storageUrl", source = "entity.storageUrl")
	@Mapping(target = "originalFilename", source = "entity.originalFilename")
	@Mapping(target = "fileSize", source = "entity.fileSize")
	@Mapping(target = "contentType", source = "entity.contentType")
	@Mapping(target = "createdAt", source = "entity.createdAt")
	FileResponse toResponse(Long postId, FilesEntity entity);
}
