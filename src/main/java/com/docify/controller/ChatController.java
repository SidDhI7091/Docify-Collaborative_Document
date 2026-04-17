package com.docify.controller;

import com.docify.service.ChatService;
import com.docify.service.ShareService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final ShareService shareService;

    @PostMapping
    public Mono<ResponseEntity<?>> chat(@RequestBody Map<String, Object> body,
                                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return Mono.just(ResponseEntity.status(401).build());

        String message = (String) body.get("message");
        String documentContent = (String) body.get("documentContent");
        Long documentId = body.get("documentId") != null 
            ? Long.valueOf(body.get("documentId").toString()) 
            : null;

        if (message == null || message.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "Message required")));
        }

        // Check access if documentId provided
        if (documentId != null && !shareService.canView(documentId, userId)) {
            return Mono.just(ResponseEntity.status(403)
                .body(Map.of("error", "Access denied")));
        }

        return chatService.chat(message, documentContent, null)
            .map(response -> ResponseEntity.ok(Map.of("response", response)));
    }
}
