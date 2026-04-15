package com.docify.controller;

import com.docify.model.*;
import com.docify.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @PostMapping("/{docId}/user")
    public ResponseEntity<?> shareWithUser(@PathVariable Long docId,
                                            @RequestBody Map<String, String> body,
                                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            DocumentPermission perm = shareService.shareWithUser(
                docId, body.get("email"), body.get("role"), userId);
            return ResponseEntity.ok(Map.of("message", "Shared successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{docId}/link")
    public ResponseEntity<?> createLink(@PathVariable Long docId,
                                         @RequestBody Map<String, String> body,
                                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        DocumentLink link = shareService.createShareLink(docId, body.get("role"));
        return ResponseEntity.ok(Map.of(
            "token", link.getToken(),
            "role", link.getPermissionType(),
            "url", "/share/" + link.getToken()
        ));
    }

    @GetMapping("/{docId}/permissions")
    public ResponseEntity<?> getPermissions(@PathVariable Long docId,
                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(shareService.getPermissionsForDoc(docId)
            .stream().map(p -> Map.of(
                "id", p.getId(),
                "userName", p.getUser().getName(),
                "userEmail", p.getUser().getEmail(),
                "role", p.getPermissionType()
            )).toList());
    }

    @DeleteMapping("/permission/{permId}")
    public ResponseEntity<?> revoke(@PathVariable Long permId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        shareService.revokePermission(permId);
        return ResponseEntity.ok(Map.of("message", "Revoked"));
    }

    @GetMapping("/view/{token}")
    public ResponseEntity<?> viewByToken(@PathVariable String token) {
        return shareService.getLinkByToken(token)
            .filter(l -> l.getIsActive())
            .map(l -> ResponseEntity.ok((Object) Map.of(
                "documentId", l.getDocument().getId(),
                "role", l.getPermissionType()
            )))
            .orElse(ResponseEntity.status(404)
                .body(Map.of("error", "Link invalid or expired")));
    }
}