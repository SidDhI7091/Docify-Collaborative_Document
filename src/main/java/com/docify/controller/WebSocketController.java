package com.docify.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/edit/{tabId}")
    public void handleEdit(@DestinationVariable Long tabId,
                           @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
            "/topic/tab/" + tabId, payload);
    }

    @MessageMapping("/cursor/{tabId}")
    public void handleCursor(@DestinationVariable Long tabId,
                              @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
            "/topic/cursor/" + tabId, payload);
    }

    @MessageMapping("/tab-event/{docId}")
    public void handleTabEvent(@DestinationVariable Long docId,
                                @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
            "/topic/tabs/" + docId, payload);
    }
}