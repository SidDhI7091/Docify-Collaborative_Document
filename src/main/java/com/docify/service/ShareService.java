package com.docify.service;

import com.docify.model.*;
import com.docify.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ShareService {
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentLinkRepository linkRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Transactional
    public DocumentPermission shareWithUser(Long docId, String email,
                                             String role, Long sharedById) {
        Document doc = documentRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        User target = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User with that email not found"));
        User sharedBy = userRepository.findById(sharedById)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<DocumentPermission> existing =
            permissionRepository.findByDocumentIdAndUserId(docId, target.getId());
        if (existing.isPresent()) {
            existing.get().setPermissionType(role);
            return permissionRepository.save(existing.get());
        }
        DocumentPermission perm = DocumentPermission.builder()
            .document(doc).user(target).permissionType(role)
            .grantedBy(sharedBy).build();
        return permissionRepository.save(perm);
    }

    @Transactional
    public DocumentLink createShareLink(Long docId, String role) {
        Document doc = documentRepository.findById(docId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        String token = UUID.randomUUID().toString().replace("-", "");
        DocumentLink link = DocumentLink.builder()
            .document(doc).token(token)
            .permissionType(role).isActive(true).build();
        return linkRepository.save(link);
    }

    public Optional<DocumentLink> getLinkByToken(String token) {
        return linkRepository.findByToken(token);
    }

    public List<DocumentPermission> getPermissionsForDoc(Long docId) {
        return permissionRepository.findByDocumentId(docId);
    }

    @Transactional
    public void revokePermission(Long permId) {
        permissionRepository.deleteById(permId);
    }

    @Transactional
    public void deactivateLink(Long linkId) {
        linkRepository.findById(linkId).ifPresent(l -> {
            l.setIsActive(false);
            linkRepository.save(l);
        });
    }

    public boolean canEdit(Long docId, Long userId) {
        Document doc = documentRepository.findById(docId).orElse(null);
        if (doc == null) return false;
        if (doc.getOwner().getId().equals(userId)) return true;
        return permissionRepository.findByDocumentIdAndUserId(docId, userId)
            .map(p -> p.getPermissionType().equals("EDIT"))
            .orElse(false);
    }

    public boolean canView(Long docId, Long userId) {
        Document doc = documentRepository.findById(docId).orElse(null);
        if (doc == null) return false;
        if (doc.getOwner().getId().equals(userId)) return true;
        return permissionRepository.existsByDocumentIdAndUserId(docId, userId);
    }
}