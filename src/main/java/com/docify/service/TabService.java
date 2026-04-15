package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class TabService {
    private final DocumentTabRepository tabRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final UserRepository userRepository;

    public List<DocumentTab> getTabsForDocument(Long documentId) {
        return tabRepository.findByDocumentIdOrderByPositionAsc(documentId);
    }

    @Transactional
    public DocumentTab createTab(Long documentId, String name) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        int count = tabRepository.countByDocumentId(documentId);
        DocumentTab tab = DocumentTab.builder()
            .document(doc).name(name).position(count).content("").build();
        return tabRepository.save(tab);
    }

    @Transactional
    public DocumentTab renameTab(Long tabId, String name) {
        DocumentTab tab = tabRepository.findById(tabId)
            .orElseThrow(() -> new RuntimeException("Tab not found"));
        tab.setName(name);
        return tabRepository.save(tab);
    }

    @Transactional
    public DocumentTab saveContent(Long tabId, String content, Long userId) {
        DocumentTab tab = tabRepository.findById(tabId)
            .orElseThrow(() -> new RuntimeException("Tab not found"));
        // save version snapshot
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        DocumentVersion version = DocumentVersion.builder()
            .document(tab.getDocument()).tab(tab)
            .savedBy(user).content(tab.getContent())
            .build();
        versionRepository.save(version);
        tab.setContent(content);
        return tabRepository.save(tab);
    }

    @Transactional
    public void deleteTab(Long tabId) {
        tabRepository.deleteById(tabId);
    }

    @Transactional
    public void reorderTabs(List<Long> tabIds) {
        for (int i = 0; i < tabIds.size(); i++) {
            Long tabId = tabIds.get(i);
            tabRepository.findById(tabId).ifPresent(tab -> {
                tab.setPosition(tabIds.indexOf(tabId));
                tabRepository.save(tab);
            });
        }
    }
}