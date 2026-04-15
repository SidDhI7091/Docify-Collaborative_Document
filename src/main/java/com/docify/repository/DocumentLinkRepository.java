// DocumentLinkRepository.java
package com.docify.repository;
import com.docify.model.DocumentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentLinkRepository extends JpaRepository<DocumentLink, Long> {
    Optional<DocumentLink> findByToken(String token);
    List<DocumentLink> findByDocumentId(Long documentId);
}