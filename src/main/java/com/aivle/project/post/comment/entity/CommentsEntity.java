package com.aivle.project.post.comment.entity;

import com.aivle.project.common.entity.BaseEntity;
import com.aivle.project.post.entity.PostsEntity;
import com.aivle.project.auth.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * comments 테이블에 매핑되는 댓글 엔티티 (계층형 - Adjacency List Model).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comments")
public class CommentsEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private PostsEntity post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private CommentsEntity parent;

	@Lob
	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "depth", nullable = false)
	private int depth = 0;

	@Column(name = "sequence", nullable = false)
	private int sequence = 0;

	@OneToMany(mappedBy = "parent")
	private List<CommentsEntity> replies = new ArrayList<>();

	public static CommentsEntity create(
		PostsEntity post,
		UserEntity user,
		CommentsEntity parent,
		String content,
		int depth,
		int sequence
	) {
		CommentsEntity comment = new CommentsEntity();
		comment.post = post;
		comment.user = user;
		comment.parent = parent;
		comment.content = content;
		comment.depth = depth;
		comment.sequence = sequence;
		return comment;
	}

	public void update(String content) {
		this.content = content;
	}

	public void markDeleted() {
		delete();
	}
}
