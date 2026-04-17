// DocumentRepository.java
package com.docify.repository;
import com.docify.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    List<Document> findByOwnerId(Long ownerId);
    
    // Fetch document with owner eagerly loaded
    @Query("SELECT d FROM Document d JOIN FETCH d.owner WHERE d.id = :id")
    Optional<Document> findByIdWithOwner(@Param("id") Long id);
    
    // docs shared with this user - returns Document with permission info
    @Query("SELECT dp.document, dp.permissionType FROM DocumentPermission dp WHERE dp.user.id = :userId ORDER BY dp.document.updatedAt DESC")
    List<Object[]> findSharedWithUserAndPermission(Long userId);
}