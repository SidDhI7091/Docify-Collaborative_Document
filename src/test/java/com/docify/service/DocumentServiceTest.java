package com.docify.service;

import com.docify.model.Document;
import com.docify.model.DocumentPermission;
import com.docify.model.User;
import com.docify.repository.DocumentPermissionRepository;
import com.docify.repository.DocumentRepository;
import com.docify.repository.DocumentTabRepository;
import com.docify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository         documentRepository;
    @Mock private UserRepository             userRepository;
    @Mock private DocumentPermissionRepository permissionRepository;
    @Mock private DocumentTabRepository      tabRepository;

    @InjectMocks
    private DocumentService documentService;

    private User     testUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@docify.com");

        testDocument = new Document();
        testDocument.setId(10L);
        testDocument.setTitle("Test Document");
        testDocument.setOwner(testUser);
    }

    // ── createDocument ────────────────────────────────────────────────

    @Test
    @DisplayName("createDocument: should create and return a new document")
    void createDocument_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        Document result = documentService.createDocument(1L, "Test Document");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Document");
        assertThat(result.getOwner().getId()).isEqualTo(1L);
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("createDocument: should throw if user not found")
    void createDocument_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(99L, "Doc"))
            .isInstanceOf(RuntimeException.class);

        verify(documentRepository, never()).save(any());
    }

    // ── getDocument ───────────────────────────────────────────────────

    @Test
    @DisplayName("getDocument: should return document by id")
    void getDocument_found() {
        when(documentRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(testDocument));

        Document result = documentService.getDocument(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Test Document");
    }

    @Test
    @DisplayName("getDocument: should throw if document does not exist")
    void getDocument_notFound_throws() {
        when(documentRepository.findByIdWithOwner(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(999L))
            .isInstanceOf(RuntimeException.class);
    }

    // ── canAccess ─────────────────────────────────────────────────────

    @Test
    @DisplayName("canAccess: owner should always have access")
    void canAccess_owner_returnsTrue() {
        when(documentRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(testDocument));

        boolean result = documentService.canAccess(10L, 1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccess: shared user should have access via permission")
    void canAccess_sharedUser_returnsTrue() {
        User otherUser = new User();
        otherUser.setId(2L);

        DocumentPermission perm = new DocumentPermission();
        perm.setDocument(testDocument);
        perm.setUser(otherUser);

        when(documentRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(testDocument));
        when(permissionRepository.findByDocumentIdAndUserId(10L, 2L))
            .thenReturn(Optional.of(perm));

        boolean result = documentService.canAccess(10L, 2L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccess: random user without permission should be denied")
    void canAccess_unauthorized_returnsFalse() {
        when(documentRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(testDocument));
        when(permissionRepository.findByDocumentIdAndUserId(10L, 99L))
            .thenReturn(Optional.empty());

        boolean result = documentService.canAccess(10L, 99L);

        assertThat(result).isFalse();
    }

    // ── getMyDocuments ────────────────────────────────────────────────

    @Test
    @DisplayName("getMyDocuments: should return documents owned by user")
    void getMyDocuments_returnsList() {
        when(documentRepository.findByOwnerIdOrderByUpdatedAtDesc(1L))
            .thenReturn(List.of(testDocument));

        List<Document> docs = documentService.getMyDocuments(1L);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getTitle()).isEqualTo("Test Document");
    }

    // ── updateTitle ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateTitle: should update and return document with new title")
    void updateTitle_success() {
        when(documentRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document result = documentService.updateTitle(10L, "New Title");

        assertThat(result.getTitle()).isEqualTo("New Title");
    }
}
