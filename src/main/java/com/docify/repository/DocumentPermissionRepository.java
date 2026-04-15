// DocumentPermissionRepository.java
package com.docify.repository;
import com.docify.model.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);
    List<DocumentPermission> findByDocumentId(Long documentId);
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);
}