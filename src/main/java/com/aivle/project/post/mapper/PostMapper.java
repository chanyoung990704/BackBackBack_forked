package com.aivle.project.post.mapper;

import com.aivle.project.comment.entity.CommentsEntity;
import com.aivle.project.common.util.NameMaskingUtil;
import com.aivle.project.post.dto.PostResponse;
import com.aivle.project.post.dto.QaReplyResponse;
import com.aivle.project.post.entity.PostsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class PostMapper {

	// 계산된 조회수 및 isPinned 필드를 응답 규격에 맞게 매핑한다.
	@Mapping(target = "name", source = "user.name", qualifiedByName = "maskName")
	@Mapping(target = "categoryId", source = "category.id")
	@Mapping(target = "viewCount", source = "viewCount")
	@Mapping(target = "isPinned", source = "pinned")
	@Mapping(target = "qnaStatus", ignore = true)
	protected abstract PostResponse toBaseResponse(PostsEntity post);

	public PostResponse toResponse(PostsEntity post) {
		return toResponseWithQnaStatus(post, null);
	}

	public PostResponse toResponseWithQnaStatus(PostsEntity post, String qnaStatus) {
		PostResponse response = toBaseResponse(post);
		// record 응답에 qnaStatus만 주입하기 위해 builder 재생성
		return PostResponse.builder()
			.id(response.id())
			.name(response.name())
			.categoryId(response.categoryId())
			.title(response.title())
			.content(response.content())
			.viewCount(response.viewCount())
			.isPinned(response.isPinned())
			.status(response.status())
			.qnaStatus(qnaStatus)
			.createdAt(response.createdAt())
			.updatedAt(response.updatedAt())
			.build();
	}

	@Mapping(target = "name", source = "user.name", qualifiedByName = "maskName")
	@Mapping(target = "postId", source = "post.id")
	public abstract QaReplyResponse toQaReplyResponse(CommentsEntity comment);

	@Named("maskName")
	protected String maskName(String name) {
		return NameMaskingUtil.mask(name);
	}
}
