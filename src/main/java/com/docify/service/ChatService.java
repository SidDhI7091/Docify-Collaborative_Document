package com.docify.service;

import com.docify.model.DocumentTab;
import com.docify.repository.DocumentTabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final DocumentTabRepository tabRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder().build();

    public Mono<String> chat(String userMessage, String documentContent, Long tabId) {
        // Build system context
        String systemPrompt = buildSystemPrompt(documentContent);
        
        // Gemini API request format
        Map<String, Object> request = Map.of(
            "contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(
                        Map.of("text", systemPrompt + "\n\nUser: " + userMessage)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 500
            )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

        return webClient.post()
            .uri(url)
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(this::extractResponse)
            .onErrorResume(e -> {
                System.err.println("Gemini API error: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("429")) {
                    return Mono.just("Rate limit reached. Please wait a moment and try again.");
                }
                if (e.getMessage() != null && e.getMessage().contains("400")) {
                    return Mono.just("Invalid request. Please try rephrasing your message.");
                }
                return Mono.just("Sorry, I encountered an error. Please try again.");
            });
    }

    private String buildSystemPrompt(String documentContent) {
        String cleanContent = stripHtml(documentContent);
        if (cleanContent.isBlank()) {
            return "You are a helpful AI assistant for Docify. The document is currently empty. Help the user get started with writing.";
        }
        
        return "You are a helpful AI assistant for Docify document editor.\n\n" +
               (cleanContent.isBlank() ? "The document is empty.\n\n" :
               "Document content:\n---\n" +
               (cleanContent.length() > 800 ? cleanContent.substring(0, 800) + "..." : cleanContent) +
               "\n---\n\n") +
               "Be concise. Max 200 words in response.";
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    @SuppressWarnings("unchecked")
    private String extractResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "No response from AI";
            
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            return "Error parsing AI response";
        }
    }
}
