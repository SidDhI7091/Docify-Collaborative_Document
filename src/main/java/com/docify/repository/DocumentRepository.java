// DocumentRepository.java
package com.docify.repository;
import com.docify.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    // docs shared with this user
    @Query("SELECT dp.document FROM DocumentPermission dp WHERE dp.user.id = :userId ORDER BY dp.document.updatedAt DESC")
    List<Document> findSharedWithUser(Long userId);
}