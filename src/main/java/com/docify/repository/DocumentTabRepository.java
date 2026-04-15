// DocumentTabRepository.java
package com.docify.repository;
import com.docify.model.DocumentTab;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentTabRepository extends JpaRepository<DocumentTab, Long> {
    List<DocumentTab> findByDocumentIdOrderByPositionAsc(Long documentId);
    int countByDocumentId(Long documentId);
}