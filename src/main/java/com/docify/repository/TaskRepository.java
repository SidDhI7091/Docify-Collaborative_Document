package com.docify.repository;

import com.docify.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t JOIN FETCH t.document JOIN FETCH t.createdBy WHERE t.document.id = :docId ORDER BY t.createdAt DESC")
    List<Task> findByDocumentIdOrderByCreatedAtDesc(Long docId);

    @Query("SELECT ta.task FROM TaskAssignee ta JOIN FETCH ta.task.document JOIN FETCH ta.task.createdBy WHERE ta.user.id = :userId ORDER BY ta.task.createdAt DESC")
    List<Task> findTasksAssignedToUser(Long userId);

    @Query("SELECT t FROM Task t JOIN FETCH t.document JOIN FETCH t.createdBy WHERE t.createdBy.id = :createdById ORDER BY t.createdAt DESC")
    List<Task> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

    @Query("SELECT t FROM Task t JOIN FETCH t.document JOIN FETCH t.createdBy WHERE t.id = :id")
    Optional<Task> findByIdWithRelations(Long id);
}