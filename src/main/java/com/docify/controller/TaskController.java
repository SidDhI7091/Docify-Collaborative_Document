package com.docify.controller;

import com.docify.service.ShareService;
import com.docify.service.TaskService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService  taskService;
    private final ShareService shareService;

    @GetMapping("/document/{docId}")
    public ResponseEntity<?> listForDocument(@PathVariable Long docId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canView(docId, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(taskService.getTasksForDocument(docId));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myTasks(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(taskService.getMyTasks(userId));
    }

    @GetMapping("/created-by-me")
    public ResponseEntity<?> createdByMe(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(taskService.getCreatedByMe(userId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @PostMapping("/document/{docId}")
    public ResponseEntity<?> create(@PathVariable Long docId,
                                     @RequestBody Map<String, Object> body,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(docId, userId))
            return ResponseEntity.status(403).body(Map.of("error", "No edit permission"));

        String title = (String) body.get("title");
        if (title == null || title.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Title required"));

        LocalDateTime due = null;
        Object dueDateObj = body.get("dueDate");
        if (dueDateObj instanceof String ds && !ds.isBlank()) {
            try { due = LocalDateTime.parse(ds, DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
            catch (Exception ignored) {}
        }

        @SuppressWarnings("unchecked")
        List<String> emails = (List<String>) body.getOrDefault("assigneeEmails", List.of());
        String description = (String) body.getOrDefault("description", "");

        try {
            Map<String, Object> result = taskService.createTask(
                docId, userId, title, description, due, emails);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error",
                e.getMessage() != null ? e.getMessage() : "Internal error"));
        }
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long taskId,
                                           @RequestBody Map<String, String> body,
                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        String newStatus = body.getOrDefault("status", "PENDING");
        if (!newStatus.equals("PENDING") && !newStatus.equals("COMPLETED"))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        try {
            return ResponseEntity.ok(taskService.updateStatus(taskId, userId, newStatus));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> delete(@PathVariable Long taskId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            taskService.deleteTask(taskId, userId);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
