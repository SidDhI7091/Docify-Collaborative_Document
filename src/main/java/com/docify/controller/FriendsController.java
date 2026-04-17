package com.docify.controller;

import com.docify.model.User;
import com.docify.repository.DocumentPermissionRepository;
import com.docify.service.PresenceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendsController {
    private final DocumentPermissionRepository permissionRepository;
    private final PresenceService presenceService;

    /**
     * Friends = all users who share at least one document with me
     * (either I shared with them, or they shared with me).
     */
    @GetMapping
    public ResponseEntity<?> getFriends(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        // People I shared docs with (they have permission on my docs)
        List<User> sharedWithOthers = permissionRepository.findUsersISharedWith(userId);
        // People who shared docs with me (they granted me permission)
        List<User> sharedWithMe = permissionRepository.findUsersWhoSharedWithMe(userId);

        // Merge, deduplicate
        Map<Long, User> friendMap = new LinkedHashMap<>();
        sharedWithOthers.forEach(u -> friendMap.put(u.getId(), u));
        sharedWithMe.forEach(u -> friendMap.put(u.getId(), u));

        return ResponseEntity.ok(friendMap.values().stream().map(u -> {
            boolean online = presenceService.isOnline(u.getId());
            Long secs = presenceService.secondsSince(u.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",     u.getId());
            m.put("name",   u.getName());
            m.put("online", online);
            m.put("lastSeenSeconds", secs);
            return m;
        }).toList());
    }

    /** Heartbeat — called by dashboard every 60s to mark user as online */
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        presenceService.heartbeat(userId);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
