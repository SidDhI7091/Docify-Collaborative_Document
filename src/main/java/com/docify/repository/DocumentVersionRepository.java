// DocumentVersionRepository.java
package com.docify.repository;
import com.docify.model.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByTabIdOrderByCreatedAtDesc(Long tabId);
}