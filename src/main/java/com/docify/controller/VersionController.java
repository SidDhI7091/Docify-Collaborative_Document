package com.docify.controller;

import com.docify.model.DocumentVersion;
import com.docify.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
public class VersionController {
    private final VersionService versionService;
    private final ShareService shareService;

    @GetMapping("/tab/{tabId}")
    public ResponseEntity<?> getVersions(@PathVariable Long tabId,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(versionService.getVersionsForTab(tabId)
            .stream().map(this::toMap).toList());
    }

    @PostMapping("/{versionId}/restore")
    public ResponseEntity<?> restore(@PathVariable Long versionId,
                                      HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            Map<String, Object> result = versionService.restoreVersion(versionId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg == null && e.getCause() != null) msg = e.getCause().getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            return ResponseEntity.status(500).body(Map.of("error", msg));
        }
    }

    @PutMapping("/{versionId}/name")
    public ResponseEntity<?> nameVersion(@PathVariable Long versionId,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(toMap(
            versionService.nameVersion(versionId, body.get("name"))));
    }

    private Map<String, Object> toMap(DocumentVersion v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("content", v.getContent());
        m.put("versionName", v.getVersionName());
        m.put("savedAt", v.getCreatedAt());
        m.put("savedBy", v.getSavedBy() != null ? v.getSavedBy().getName() : "Unknown");
        return m;
    }
}