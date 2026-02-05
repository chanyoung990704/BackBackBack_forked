package com.aivle.project.qna.mapper;

import com.aivle.project.comment.entity.CommentsEntity;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.qna.dto.QaPostResponse;
import com.aivle.project.qna.dto.QaReplyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface QnaMapper {

	@Mapping(target = "id", expression = "java(String.valueOf(post.getId()))")
	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "body", source = "content")
	@Mapping(target = "author", source = "user.name")
	@Mapping(target = "status", source = "post", qualifiedByName = "calculateStatus")
	@Mapping(target = "replies", source = "replies")
	@Mapping(target = "tags", ignore = true) // 태그 기능은 추후 확장
	QaPostResponse toResponse(PostsEntity post);

	@Mapping(target = "id", expression = "java(String.valueOf(comment.getId()))")
	@Mapping(target = "author", source = "user.name")
	@Mapping(target = "body", source = "content")
	QaReplyResponse toReplyResponse(CommentsEntity comment);

	@Named("calculateStatus")
	default String calculateStatus(PostsEntity post) {
		return post.getReplies() != null && !post.getReplies().isEmpty() ? "answered" : "pending";
	}
}
