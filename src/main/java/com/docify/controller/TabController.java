package com.docify.controller;

import com.docify.model.DocumentTab;
import com.docify.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/documents/{docId}/tabs")
@RequiredArgsConstructor
public class TabController {
    private final TabService tabService;
    private final ShareService shareService;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long docId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canView(docId, userId))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(tabService.getTabsForDocument(docId)
            .stream().map(this::toMap).toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long docId,
                                     @RequestBody Map<String, String> body,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(docId, userId))
            return ResponseEntity.status(403).build();
        String name = body.getOrDefault("name", "New Tab");
        return ResponseEntity.ok(toMap(tabService.createTab(docId, name)));
    }

    @PutMapping("/{tabId}/rename")
    public ResponseEntity<?> rename(@PathVariable Long docId,
                                     @PathVariable Long tabId,
                                     @RequestBody Map<String, String> body,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(docId, userId))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(toMap(tabService.renameTab(tabId, body.get("name"))));
    }

    @PutMapping("/{tabId}/content")
    public ResponseEntity<?> saveContent(@PathVariable Long docId,
                                          @PathVariable Long tabId,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(docId, userId))
            return ResponseEntity.status(403).build();
        return ResponseEntity.ok(toMap(
            tabService.saveContent(tabId, body.get("content"), userId)));
    }

    @DeleteMapping("/{tabId}")
    public ResponseEntity<?> delete(@PathVariable Long docId,
                                     @PathVariable Long tabId,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (!shareService.canEdit(docId, userId))
            return ResponseEntity.status(403).build();
        tabService.deleteTab(tabId);
        return ResponseEntity.ok(Map.of("message", "Tab deleted"));
    }

    @PutMapping("/reorder")
    public ResponseEntity<?> reorder(@PathVariable Long docId,
                                      @RequestBody Map<String, List<Long>> body,
                                      HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        tabService.reorderTabs(body.get("tabIds"));
        return ResponseEntity.ok(Map.of("message", "Reordered"));
    }

    private Map<String, Object> toMap(DocumentTab t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("content", t.getContent());
        m.put("position", t.getPosition());
        m.put("documentId", t.getDocument().getId());
        return m;
    }
}