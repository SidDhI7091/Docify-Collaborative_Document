// CommentRepository.java
package com.docify.repository;
import com.docify.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTabIdAndParentIsNullOrderByCreatedAtAsc(Long tabId);
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}