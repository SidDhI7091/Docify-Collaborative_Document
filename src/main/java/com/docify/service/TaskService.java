package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository         taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final DocumentRepository     documentRepository;
    private final UserRepository         userRepository;
    private final NotificationService    notificationService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTasksForDocument(Long docId) {
        return taskRepository.findByDocumentIdOrderByCreatedAtDesc(docId)
            .stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyTasks(Long userId) {
        return taskRepository.findTasksAssignedToUser(userId)
            .stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCreatedByMe(Long userId) {
        try {
            return taskRepository.findByCreatedByIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toMap).toList();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public Map<String, Object> createTask(Long docId, Long creatorId,
                                           String title, String description,
                                           LocalDateTime dueDate, List<String> assigneeEmails) {
        Document doc = documentRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Task task = Task.builder()
            .document(doc).createdBy(creator)
            .title(title)
            .description(description != null ? description : "")
            .dueDate(dueDate)
            .status("PENDING")
            .build();
        task = taskRepository.save(task);

        if (assigneeEmails != null) {
            for (String email : assigneeEmails) {
                User assignee = userRepository.findByEmail(email.trim()).orElse(null);
                if (assignee != null) {
                    TaskAssignee ta = TaskAssignee.builder()
                        .task(task).user(assignee).build();
                    taskAssigneeRepository.save(ta);
                    notificationService.notifyTaskAssigned(assignee, creator, task, doc);
                    System.out.println("Assigned task " + task.getId() + " to " + email);
                } else {
                    System.out.println("No user found for email: " + email);
                }
            }
        }

        // Reload with relations
        return toMap(taskRepository.findByIdWithRelations(task.getId()).orElse(task));
    }

    @Transactional
    public Map<String, Object> updateStatus(Long taskId, Long userId, String newStatus) {
        Task task = taskRepository.findByIdWithRelations(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(newStatus);
        taskRepository.save(task);

        if ("COMPLETED".equals(newStatus)) {
            User completedBy = userRepository.findById(userId).orElse(null);
            if (completedBy != null) {
                notificationService.notifyTaskCompleted(completedBy, task, task.getDocument());
            }
        }
        return toMap(task);
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdWithRelations(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
        if (!task.getCreatedBy().getId().equals(userId))
            throw new RuntimeException("Only the creator can delete a task");
        taskRepository.delete(task);
    }

    public Map<String, Object> toMap(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           t.getId());
        m.put("title",        t.getTitle());
        m.put("description",  t.getDescription());
        m.put("status",       t.getStatus());
        m.put("dueDate",      t.getDueDate());
        m.put("createdAt",    t.getCreatedAt());
        m.put("documentId",   t.getDocument().getId());
        m.put("documentTitle",t.getDocument().getTitle());
        if (t.getCreatedBy() != null) m.put("createdByName", t.getCreatedBy().getName());
        List<TaskAssignee> assignees = taskAssigneeRepository.findByTaskId(t.getId());
        m.put("assignees", assignees.stream().map(a -> Map.of(
            "id",    a.getUser().getId(),
            "name",  a.getUser().getName(),
            "email", a.getUser().getEmail()
        )).toList());
        return m;
    }
}
