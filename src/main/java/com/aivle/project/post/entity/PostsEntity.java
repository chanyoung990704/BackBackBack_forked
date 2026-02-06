package com.aivle.project.post.entity;

import com.aivle.project.category.entity.CategoriesEntity;
import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * posts 테이블에 매핑되는 게시글 엔티티.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "posts")
public class PostsEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private CategoriesEntity category;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Lob
	@Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	@OneToOne(mappedBy = "post", fetch = FetchType.LAZY)
	private PostViewCountsEntity viewCountEntity;

	@jakarta.persistence.OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
	private java.util.List<com.aivle.project.comment.entity.CommentsEntity> replies = new java.util.ArrayList<>();

	@Column(name = "is_pinned", nullable = false)
	private boolean isPinned = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PostStatus status = PostStatus.PUBLISHED;

	@PrePersist
	private void prePersist() {
		if (status == null) {
			status = PostStatus.PUBLISHED;
		}
	}

	/**
	 * 게시글 생성.
	 */
	public static PostsEntity create(
		UserEntity user,
		CategoriesEntity category,
		String title,
		String content,
		boolean isPinned,
		PostStatus status
	) {
		PostsEntity post = new PostsEntity();
		post.user = user;
		post.category = category;
		post.title = title;
		post.content = content;
		post.isPinned = isPinned;
		post.status = status != null ? status : PostStatus.PUBLISHED;
		return post;
	}

	/**
	 * 게시글 수정.
	 */
	public void update(String title, String content, CategoriesEntity category, Boolean isPinned, PostStatus status) {
		this.title = title;
		this.content = content;
		this.category = category;
		if (isPinned != null) {
			this.isPinned = isPinned;
		}
		if (status != null) {
			this.status = status;
		}
	}

	/**
	 * 게시글 소프트 삭제.
	 */
	public void markDeleted() {
		delete();
	}

	/**
	 * 조회수 조회 (없으면 0 반환).
	 */
	public int getViewCount() {
		return viewCountEntity != null ? viewCountEntity.getViewCount() : 0;
	}
}
