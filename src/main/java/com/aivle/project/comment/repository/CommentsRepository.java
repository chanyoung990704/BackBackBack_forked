package com.aivle.project.comment.repository;

import com.aivle.project.comment.entity.CommentsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentsRepository extends JpaRepository<CommentsEntity, Long> {
    List<CommentsEntity> findByPostIdAndDeletedAtIsNullOrderByDepthAscSequenceAsc(Long postId);

    @Query("SELECT COALESCE(MAX(c.sequence), -1) FROM CommentsEntity c WHERE c.post.id = :postId AND c.parent IS NULL")
    int findMaxSequenceByPostIdAndParentIsNull(@Param("postId") Long postId);

    @Query("SELECT COALESCE(MAX(c.sequence), -1) FROM CommentsEntity c WHERE c.parent.id = :parentId")
    int findMaxSequenceByParentId(@Param("parentId") Long parentId);
}
