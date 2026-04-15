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
        if (userId == null) return ResponseEntity.status(400).build(); // test expects 4xx client error
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
        List<Map<String, Object>> shared = documentService.getSharedDocuments(userId)
            .stream().map(this::toMap).toList();
        return ResponseEntity.ok(Map.of("myDocuments", owned, "sharedDocuments", shared));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!documentService.canAccess(id, userId))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(toMap(documentService.getDocument(id)));
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

    private Map<String, Object> toMap(Document d) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (d == null) return m;
        m.put("id", d.getId());
        m.put("title", d.getTitle());
        if (d.getOwner() != null) {
            m.put("ownerId", d.getOwner().getId());
            m.put("ownerName", d.getOwner().getName());
        }
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        return m;
    }
}