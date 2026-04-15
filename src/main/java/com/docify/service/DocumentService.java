package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentTabRepository tabRepository;
    private final UserRepository userRepository;

    @Transactional
    public Document createDocument(Long ownerId, String title) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Document doc = Document.builder()
            .owner(owner).title(title).build();
        doc = documentRepository.save(doc);
        DocumentTab defaultTab = DocumentTab.builder()
            .document(doc).name("Tab 1").position(0).content("").build();
        tabRepository.save(defaultTab);
        return doc;
    }

    public List<Document> getMyDocuments(Long ownerId) {
        return documentRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    public List<Document> getSharedDocuments(Long userId) {
        return documentRepository.findSharedWithUser(userId);
    }

    public Document getDocument(Long docId) {
        return documentRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    @Transactional
    public Document updateTitle(Long docId, String title) {
        Document doc = getDocument(docId);
        doc.setTitle(title);
        return documentRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long docId) {
        documentRepository.deleteById(docId);
    }

    public boolean hasAccess(Long docId, Long userId, String requiredRole) {
        Document doc = getDocument(docId);
        if (doc.getOwner().getId().equals(userId)) return true;
        var perm = doc.getTabs(); // just a check — real check below
        return documentRepository.findById(docId)
            .map(d -> d.getOwner().getId().equals(userId))
            .orElse(false);
    }
}