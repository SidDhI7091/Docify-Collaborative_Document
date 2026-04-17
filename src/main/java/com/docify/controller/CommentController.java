package com.docify.controller;

import com.docify.model.*;
import com.docify.repository.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentRepository commentRepository;
    private final DocumentTabRepository tabRepository;
    private final UserRepository userRepository;

    /** GET /api/comments/tab/{tabId} — top-level comments for a tab */
    @GetMapping("/tab/{tabId}")
    public ResponseEntity<?> listForTab(@PathVariable Long tabId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<Comment> comments = commentRepository
            .findByTabIdAndParentIsNullOrderByCreatedAtAsc(tabId);
        return ResponseEntity.ok(comments.stream().map(c -> toMap(c, true)).toList());
    }

    /** POST /api/comments/tab/{tabId} — post a new top-level comment */
    @PostMapping("/tab/{tabId}")
    public ResponseEntity<?> create(@PathVariable Long tabId,
                                     @RequestBody Map<String, String> body,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        String content = body.get("content");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Content required"));

        DocumentTab tab = tabRepository.findById(tabId)
            .orElseThrow(() -> new RuntimeException("Tab not found"));
        User author = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = Comment.builder()
            .tab(tab).author(author).content(content).resolved(false).build();
        comment = commentRepository.save(comment);
        return ResponseEntity.ok(toMap(comment, false));
    }

    /** GET /api/comments/{commentId}/replies */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<?> getReplies(@PathVariable Long commentId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        return ResponseEntity.ok(replies.stream().map(c -> toMap(c, false)).toList());
    }

    /** POST /api/comments/{commentId}/replies — post a reply */
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<?> reply(@PathVariable Long commentId,
                                    @RequestBody Map<String, String> body,
                                    HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        String content = body.get("content");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Content required"));

        Comment parent = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        User author = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Comment reply = Comment.builder()
            .tab(parent.getTab()).author(author)
            .parent(parent).content(content).resolved(false).build();
        reply = commentRepository.save(reply);
        return ResponseEntity.ok(toMap(reply, false));
    }

    /** PATCH /api/comments/{commentId}/resolve */
    @PatchMapping("/{commentId}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Long commentId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setResolved(true);
        commentRepository.save(comment);
        return ResponseEntity.ok(Map.of("resolved", true));
    }

    /** DELETE /api/comments/{commentId} — author or doc owner can delete */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long commentId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getAuthor().getId().equals(userId))
            return ResponseEntity.status(403).body(Map.of("error", "Not your comment"));
        commentRepository.delete(comment);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(Comment c, boolean includeReplyCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         c.getId());
        m.put("content",    c.getContent());
        m.put("authorName", c.getAuthor() != null ? c.getAuthor().getName()  : "Unknown");
        m.put("authorId",   c.getAuthor() != null ? c.getAuthor().getId()    : null);
        m.put("resolved",   c.getResolved() != null ? c.getResolved() : false);
        m.put("createdAt",  c.getCreatedAt());
        m.put("parentId",   c.getParent() != null ? c.getParent().getId() : null);
        if (includeReplyCount) {
            int rc = commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId()).size();
            m.put("replyCount", rc);
        }
        return m;
    }
}