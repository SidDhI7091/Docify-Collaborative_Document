package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class VersionService {
    private final DocumentVersionRepository versionRepository;
    private final DocumentTabRepository tabRepository;
    private final UserRepository userRepository;

    public List<DocumentVersion> getVersionsForTab(Long tabId) {
        return versionRepository.findByTabIdOrderByCreatedAtDesc(tabId);
    }

    @Transactional
    public Map<String, Object> restoreVersion(Long versionId, Long userId) {
        DocumentVersion version = versionRepository.findByIdWithTab(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        DocumentTab tab = version.getTab();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Snapshot current content before overwriting
        String currentContent = tab.getContent();
        if (currentContent != null && !currentContent.isBlank()) {
            DocumentVersion snapshot = DocumentVersion.builder()
                .document(tab.getDocument())
                .tab(tab)
                .savedBy(user)
                .content(currentContent)
                .versionName("Before restore")
                .build();
            versionRepository.save(snapshot);
        }

        // Restore the content
        String restoredContent = version.getContent() != null ? version.getContent() : "";
        tab.setContent(restoredContent);
        tabRepository.save(tab);

        // Return plain data — no JPA entities
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", tab.getId());
        result.put("content", restoredContent);
        return result;
    }

    @Transactional
    public DocumentVersion nameVersion(Long versionId, String name) {
        DocumentVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        version.setVersionName(name);
        return versionRepository.save(version);
    }
}