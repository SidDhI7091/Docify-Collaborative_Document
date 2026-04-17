package com.docify.repository;

import com.docify.model.TaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {
    @Query("SELECT ta FROM TaskAssignee ta JOIN FETCH ta.user WHERE ta.task.id = :taskId")
    List<TaskAssignee> findByTaskId(Long taskId);
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);
    void deleteByTaskId(Long taskId);
}