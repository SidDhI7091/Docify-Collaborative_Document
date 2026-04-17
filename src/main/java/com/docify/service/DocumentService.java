package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentTabRepository tabRepository;
    private final UserRepository userRepository;
    private final DocumentPermissionRepository permissionRepository;

    @Transactional
    public Document createDocument(Long ownerId, String title) {
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Document doc = Document.builder()
            .owner(owner).title(title).build();
        doc = documentRepository.save(doc);
        if (tabRepository != null) {
            DocumentTab defaultTab = DocumentTab.builder()
                .document(doc).name("Tab 1").position(0).content("").build();
            tabRepository.save(defaultTab);
        }
        return doc;
    }

    public List<Document> getMyDocuments(Long ownerId) {
        return documentRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    public List<Map<String, Object>> getSharedDocuments(Long userId) {
        List<Object[]> results = documentRepository.findSharedWithUserAndPermission(userId);
        return results.stream().map(row -> {
            Document doc = (Document) row[0];
            String permissionType = (String) row[1];
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", doc.getId());
            map.put("title", doc.getTitle());
            map.put("ownerId", doc.getOwner().getId());
            map.put("ownerName", doc.getOwner().getName());
            map.put("color", doc.getColor());
            map.put("createdAt", doc.getCreatedAt());
            map.put("updatedAt", doc.getUpdatedAt());
            map.put("permissionType", permissionType);
            return map;
        }).toList();
    }

    public Document getDocument(Long docId) {
        return documentRepository.findByIdWithOwner(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    @Transactional
    public Document updateTitle(Long docId, String title) {
        Document doc = getDocument(docId);
        doc.setTitle(title);
        return documentRepository.save(doc);
    }

    @Transactional
    public void updateColor(Long docId, String color) {
        Document doc = getDocument(docId);
        doc.setColor(color);
        documentRepository.save(doc);
    }

    @Transactional
    public void updateDeadline(Long docId, String deadline) {
        Document doc = getDocument(docId);
        doc.setDeadline(deadline);
        documentRepository.save(doc);
    }

    public boolean canAccess(Long documentId, Long userId) {
        Document doc = documentRepository.findByIdWithOwner(documentId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        if (doc.getOwner().getId().equals(userId)) return true;
        return permissionRepository.findByDocumentIdAndUserId(documentId, userId).isPresent();
    }

    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        Document doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        if (!doc.getOwner().getId().equals(userId))
            throw new RuntimeException("Unauthorized");
        documentRepository.delete(doc);
    }
}
