package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class VersionService {
    private final DocumentVersionRepository versionRepository;
    private final DocumentTabRepository tabRepository;
    private final UserRepository userRepository;

    public List<DocumentVersion> getVersionsForTab(Long tabId) {
        return versionRepository.findByTabIdOrderByCreatedAtDesc(tabId);
    }

    @Transactional
    public DocumentTab restoreVersion(Long versionId, Long userId) {
        DocumentVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        DocumentTab tab = version.getTab();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        // snapshot current before restoring
        DocumentVersion snapshot = DocumentVersion.builder()
            .document(tab.getDocument()).tab(tab)
            .savedBy(user).content(tab.getContent())
            .versionName("Before restore").build();
        versionRepository.save(snapshot);
        tab.setContent(version.getContent());
        return tabRepository.save(tab);
    }

    @Transactional
    public DocumentVersion nameVersion(Long versionId, String name) {
        DocumentVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        version.setVersionName(name);
        return versionRepository.save(version);
    }
}