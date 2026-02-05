package com.aivle.project.qna.service;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.category.repository.CategoriesRepository;
import com.aivle.project.comment.entity.CommentsEntity;
import com.aivle.project.comment.repository.CommentsRepository;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.post.entity.PostStatus;
import com.aivle.project.post.repository.PostsRepository;
import com.aivle.project.qna.dto.QaPostCreateRequest;
import com.aivle.project.qna.dto.QaPostResponse;
import com.aivle.project.qna.dto.QaReplyCreateRequest;
import com.aivle.project.qna.dto.QaReplyResponse;
import com.aivle.project.qna.mapper.QnaMapper;
import com.aivle.project.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

	private final PostsRepository postsRepository;
	private final CommentsRepository commentsRepository;
	private final CategoriesRepository categoriesRepository;
	private final QnaMapper qnaMapper;

	private static final String QNA_CATEGORY_NAME = "QNA";

	/**
	 * 모든 Q&A 목록 조회 (어드민용)
	 */
	public List<QaPostResponse> listAll() {
		List<PostsEntity> allPosts = postsRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();
		
		return allPosts.stream()
			.filter(post -> post.getCategory() != null && QNA_CATEGORY_NAME.equalsIgnoreCase(post.getCategory().getName()))
			.map(qnaMapper::toResponse)
			.collect(Collectors.toList());
	}

	/**
	 * 내 Q&A 목록 조회
	 */
	public List<QaPostResponse> listMyQna(UserEntity user) {
		List<PostsEntity> allPosts = postsRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc();

		return allPosts.stream()
			.filter(post -> post.getCategory() != null && QNA_CATEGORY_NAME.equalsIgnoreCase(post.getCategory().getName()))
			.filter(post -> post.getUser().getId().equals(user.getId()))
			.map(qnaMapper::toResponse)
			.collect(Collectors.toList());
	}

	/**
	 * Q&A 상세 조회
	 */
	public QaPostResponse get(Long id) {
		PostsEntity post = postsRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
		return qnaMapper.toResponse(post);
	}

	/**
	 * Q&A 생성
	 */
	@Transactional
	public QaPostResponse create(UserEntity user, QaPostCreateRequest request) {
		CategoriesEntity qnaCategory = getOrCreateQnaCategory();
		PostsEntity post = PostsEntity.create(
			user,
			qnaCategory,
			request.title(),
			request.body(),
			false,
			PostStatus.PUBLISHED
		);
		return qnaMapper.toResponse(postsRepository.save(post));
	}

	/**
	 * Q&A 답변(댓글) 추가
	 */
	@Transactional
	public QaReplyResponse addReply(UserEntity admin, Long postId, QaReplyCreateRequest request) {
		PostsEntity post = postsRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		// Q&A 답변은 뎁스 0의 댓글로 처리
		CommentsEntity comment = CommentsEntity.create(
			post,
			admin,
			null,
			request.body(),
			0,
			post.getReplies().size() + 1
		);
		
		CommentsEntity savedComment = commentsRepository.save(comment);
		// 양방향 편의 메서드 대신 직접 추가 (JPA 영속성 컨텍스트 갱신용)
		post.getReplies().add(savedComment);
		
		return qnaMapper.toReplyResponse(savedComment);
	}

	private CategoriesEntity getOrCreateQnaCategory() {
		return categoriesRepository.findByNameAndDeletedAtIsNull(QNA_CATEGORY_NAME)
			.orElseGet(() -> categoriesRepository.save(CategoriesEntity.create(
				QNA_CATEGORY_NAME,
				"Q&A 게시판",
				99,
				true
			)));
	}
}
