package com.docify.service;

import com.docify.model.*;
import com.docify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /** Called when a task is assigned — notifies the assignee */
    @Transactional
    public void notifyTaskAssigned(User assignee, User assignedBy,
                                    Task task, Document document) {
        String due = task.getDueDate() != null
            ? task.getDueDate().toLocalDate().toString()
            : "No deadline";

        String msg = String.format(
            "📋 %s assigned you a task \"%s\" in \"%s\" · Deadline: %s",
            assignedBy.getName(), task.getTitle(), document.getTitle(), due
        );

        Notification n = Notification.builder()
            .user(assignee).type("TASK_ASSIGNED")
            .message(msg).taskId(task.getId())
            .documentId(document.getId()).read(false)
            .build();
        notificationRepository.save(n);
    }

    /** Called when an assignee marks a task done — notifies the task creator */
    @Transactional
    public void notifyTaskCompleted(User completedBy, Task task, Document document) {
        User creator = task.getCreatedBy();
        if (creator == null || creator.getId().equals(completedBy.getId())) return;

        String msg = String.format(
            "✅ %s completed the task \"%s\" in \"%s\"",
            completedBy.getName(), task.getTitle(), document.getTitle()
        );

        Notification n = Notification.builder()
            .user(creator).type("TASK_COMPLETED")
            .message(msg).taskId(task.getId())
            .documentId(document.getId()).read(false)
            .build();
        notificationRepository.save(n);
    }

    public List<Notification> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }
}
