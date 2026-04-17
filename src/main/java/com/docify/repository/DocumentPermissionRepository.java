package com.docify.repository;
import com.docify.model.DocumentPermission;
import com.docify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);
    List<DocumentPermission> findByDocumentId(Long documentId);
    boolean existsByDocumentIdAndUserId(Long documentId, Long userId);

    /** Users I granted permission to (on docs I own) */
    @Query("SELECT DISTINCT dp.user FROM DocumentPermission dp WHERE dp.document.owner.id = :ownerId AND dp.user.id <> :ownerId")
    List<User> findUsersISharedWith(Long ownerId);

    /** Users who granted me permission (they own the doc) */
    @Query("SELECT DISTINCT dp.document.owner FROM DocumentPermission dp WHERE dp.user.id = :userId AND dp.document.owner.id <> :userId")
    List<User> findUsersWhoSharedWithMe(Long userId);
}