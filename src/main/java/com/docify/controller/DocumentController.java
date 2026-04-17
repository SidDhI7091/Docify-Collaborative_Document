package com.docify.controller;

import com.docify.model.*;
import com.docify.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;
    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body,
                                    HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(400).build();
        String title = body.getOrDefault("title", "Untitled document");
        Document doc = documentService.createDocument(userId, title);
        return ResponseEntity.ok(toMap(doc));
    }

    @GetMapping
    public ResponseEntity<?> listMine(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<Map<String, Object>> owned = documentService.getMyDocuments(userId)
            .stream().map(this::toMap).toList();
        List<Map<String, Object>> shared = documentService.getSharedDocuments(userId);
        // Use consistent keys that match the frontend JS
        return ResponseEntity.ok(Map.of("myDocuments", owned, "sharedDocuments", shared));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!documentService.canAccess(id, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

        Document doc = documentService.getDocument(id);
        Map<String, Object> response = toMap(doc);
        response.put("canEdit", shareService.canEdit(id, userId));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/title")
    public ResponseEntity<?> updateTitle(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(id, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(toMap(
            documentService.updateTitle(id, body.get("title"))));
    }

    /** Kept for backward compat — same as PUT /{id}/title */
    @PatchMapping("/{id}/rename")
    public ResponseEntity<?> renameDoc(@PathVariable Long id,
                                       @RequestBody Map<String, String> body,
                                       HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(id, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(toMap(
            documentService.updateTitle(id, body.getOrDefault("title", "Untitled"))));
    }

    /** PATCH /api/documents/{id}/color  — saves card colour preference */
    @PatchMapping("/{id}/color")
    public ResponseEntity<?> updateColor(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!documentService.canAccess(id, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        String color = body.getOrDefault("color", "#f87c7c");
        documentService.updateColor(id, color);
        return ResponseEntity.ok(Map.of("color", color));
    }

    /** POST /api/documents/{id}/deadline — stores a deadline date */
    @PostMapping("/{id}/deadline")
    public ResponseEntity<?> setDeadline(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!documentService.canAccess(id, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        String deadline = body.get("deadline");
        if (deadline == null) return ResponseEntity.badRequest().body(Map.of("error", "deadline required"));
        documentService.updateDeadline(id, deadline);
        return ResponseEntity.ok(Map.of("deadline", deadline));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(400).build();
        try {
            documentService.deleteDocument(id, userId);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(Document d) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (d == null) return m;
        m.put("id", d.getId());
        m.put("title", d.getTitle());
        if (d.getOwner() != null) {
            m.put("ownerId",   d.getOwner().getId());
            m.put("ownerName", d.getOwner().getName());
        }
        m.put("color",     d.getColor());
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        return m;
    }
}
