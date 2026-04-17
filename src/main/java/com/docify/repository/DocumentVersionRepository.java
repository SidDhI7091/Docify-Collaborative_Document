// DocumentVersionRepository.java
package com.docify.repository;
import com.docify.model.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByTabIdOrderByCreatedAtDesc(Long tabId);

    @Query("SELECT v FROM DocumentVersion v JOIN FETCH v.tab t JOIN FETCH t.document WHERE v.id = :id")
    Optional<DocumentVersion> findByIdWithTab(@Param("id") Long id);
}