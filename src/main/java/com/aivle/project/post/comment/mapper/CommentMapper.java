package com.aivle.project.post.comment.mapper;

import com.aivle.project.post.comment.dto.CommentResponse;
import com.aivle.project.post.comment.entity.CommentsEntity;
import com.aivle.project.common.util.NameMaskingUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

	// 연관 엔티티의 식별자를 응답 필드로 평탄화한다.
	@Mapping(target = "name", source = "user.name", qualifiedByName = "maskName")
	@Mapping(target = "postId", source = "post.id")
	@Mapping(target = "parentId", source = "parent.id")
	CommentResponse toResponse(CommentsEntity comment);

	@Named("maskName")
	default String maskName(String name) {
		return NameMaskingUtil.mask(name);
	}
}
