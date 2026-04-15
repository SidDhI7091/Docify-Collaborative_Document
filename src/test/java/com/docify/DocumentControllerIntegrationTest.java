// package com.docify;

// import com.docify.model.Document;
// import com.docify.model.User;
// import com.docify.service.DocumentService;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import com.docify.DocifyApplication;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
// import org.springframework.mock.web.MockHttpSession;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.web.servlet.MockMvc;

// import java.time.LocalDateTime;
// import java.util.List;

// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /**
//  * Integration tests for DocumentController.
//  * Tests the full HTTP layer: routing, serialization, and status codes.
//  */
// // @SpringBootTest
// @SpringBootTest(classes = DocifyApplication.class)
// @AutoConfigureMockMvc
// @ActiveProfiles("test")
// class DocumentControllerIntegrationTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @MockBean
//     private DocumentService documentService;

//     private MockHttpSession authenticatedSession(Long userId) {
//         MockHttpSession session = new MockHttpSession();
//         session.setAttribute("userId", userId);
//         return session;
//     }

//     private Document sampleDocument(Long id, String title, User owner) {
//         Document doc = new Document();
//         doc.setId(id);
//         doc.setTitle(title);
//         doc.setOwner(owner);
//         doc.setCreatedAt(LocalDateTime.now());
//         doc.setUpdatedAt(LocalDateTime.now());
//         return doc;
//     }

//     private User sampleUser(Long id, String name) {
//         User user = new User();
//         user.setId(id);
//         user.setName(name);
//         user.setEmail(name.toLowerCase() + "@docify.com");
//         return user;
//     }

//     // ── POST /api/documents ───────────────────────────────────────────

//     @Test
//     @DisplayName("POST /api/documents – authenticated user can create a document")
//     void createDocument_authenticated_returns200() throws Exception {
//         User owner = sampleUser(1L, "Alice");
//         Document doc = sampleDocument(1L, "My Doc", owner);

//         when(documentService.createDocument(eq(1L), eq("My Doc"))).thenReturn(doc);

//         mockMvc.perform(post("/api/documents")
//                 .session(authenticatedSession(1L))
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content("{\"title\":\"My Doc\"}"))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.title").value("My Doc"))
//             .andExpect(jsonPath("$.id").value(1));
//     }

//     @Test
//     @DisplayName("POST /api/documents – unauthenticated request returns 400")
//     void createDocument_unauthenticated_returns400() throws Exception {
//         mockMvc.perform(post("/api/documents")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content("{\"title\":\"My Doc\"}"))
//             .andExpect(status().is4xxClientError());
//     }

//     // ── GET /api/documents ────────────────────────────────────────────

//     @Test
//     @DisplayName("GET /api/documents – returns my documents and shared documents")
//     void listDocuments_returns200WithBothLists() throws Exception {
//         User owner = sampleUser(1L, "Alice");
//         Document doc = sampleDocument(1L, "My Doc", owner);

//         when(documentService.getMyDocuments(1L)).thenReturn(List.of(doc));
//         when(documentService.getSharedDocuments(1L)).thenReturn(List.of());

//         mockMvc.perform(get("/api/documents")
//                 .session(authenticatedSession(1L)))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.myDocuments").isArray())
//             .andExpect(jsonPath("$.sharedDocuments").isArray())
//             .andExpect(jsonPath("$.myDocuments[0].title").value("My Doc"));
//     }

//     // ── GET /api/documents/{id} ───────────────────────────────────────

//     @Test
//     @DisplayName("GET /api/documents/{id} – accessible document returns 200")
//     void getDocument_accessible_returns200() throws Exception {
//         User owner = sampleUser(1L, "Alice");
//         Document doc = sampleDocument(1L, "My Doc", owner);

//         when(documentService.canAccess(1L, 1L)).thenReturn(true);
//         when(documentService.getDocument(1L)).thenReturn(doc);

//         mockMvc.perform(get("/api/documents/1")
//                 .session(authenticatedSession(1L)))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.id").value(1))
//             .andExpect(jsonPath("$.title").value("My Doc"));
//     }

//     @Test
//     @DisplayName("GET /api/documents/{id} – forbidden document returns 403")
//     void getDocument_forbidden_returns403() throws Exception {
//         when(documentService.canAccess(1L, 2L)).thenReturn(false);

//         mockMvc.perform(get("/api/documents/1")
//                 .session(authenticatedSession(2L)))
//             .andExpect(status().isForbidden());
//     }

//     // ── DELETE /api/documents/{id} ────────────────────────────────────

//     @Test
//     @DisplayName("DELETE /api/documents/{id} – owner can delete their document")
//     void deleteDocument_owner_returns200() throws Exception {
//         doNothing().when(documentService).deleteDocument(1L, 1L);

//         mockMvc.perform(delete("/api/documents/1")
//                 .session(authenticatedSession(1L)))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.message").value("Deleted"));
//     }

//     @Test
//     @DisplayName("DELETE /api/documents/{id} – non-owner gets 400 error")
//     void deleteDocument_nonOwner_returns400() throws Exception {
//         doThrow(new RuntimeException("Not the owner"))
//             .when(documentService).deleteDocument(1L, 99L);

//         mockMvc.perform(delete("/api/documents/1")
//                 .session(authenticatedSession(99L)))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.error").exists());
//     }
// }

package com.docify;

import com.docify.controller.DocumentController;
import com.docify.model.Document;
import com.docify.model.User;
import com.docify.service.DocumentService;
import com.docify.service.ShareService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DocumentController.
 * Uses @WebMvcTest to load ONLY the web layer (no DB, no WebSocket).
 * All services are mocked with @MockBean.
 */
@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    // DocumentController also depends on ShareService — must mock it too
    @MockBean
    private ShareService shareService;

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }

    private Document sampleDocument(Long id, String title, User owner) {
        Document doc = new Document();
        doc.setId(id);
        doc.setTitle(title);
        doc.setOwner(owner);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }

    private User sampleUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@docify.com");
        return user;
    }

    // ── POST /api/documents ───────────────────────────────────────────

    @Test
    @DisplayName("POST /api/documents – authenticated user can create a document")
    void createDocument_authenticated_returns200() throws Exception {
        User owner = sampleUser(1L, "Alice");
        Document doc = sampleDocument(1L, "My Doc", owner);

        when(documentService.createDocument(eq(1L), eq("My Doc"))).thenReturn(doc);

        mockMvc.perform(post("/api/documents")
                .session(authenticatedSession(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"My Doc\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("My Doc"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/documents – unauthenticated request returns 400")
    void createDocument_unauthenticated_returns400() throws Exception {
        mockMvc.perform(post("/api/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"My Doc\"}"))
            .andExpect(status().is4xxClientError());
    }

    // ── GET /api/documents ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/documents – returns myDocuments and sharedDocuments")
    void listDocuments_returns200WithBothLists() throws Exception {
        User owner = sampleUser(1L, "Alice");
        Document doc = sampleDocument(1L, "My Doc", owner);

        when(documentService.getMyDocuments(1L)).thenReturn(List.of(doc));
        when(documentService.getSharedDocuments(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/documents")
                .session(authenticatedSession(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.myDocuments").isArray())
            .andExpect(jsonPath("$.sharedDocuments").isArray())
            .andExpect(jsonPath("$.myDocuments[0].title").value("My Doc"));
    }

    // ── GET /api/documents/{id} ───────────────────────────────────────

    @Test
    @DisplayName("GET /api/documents/{id} – accessible document returns 200")
    void getDocument_accessible_returns200() throws Exception {
        User owner = sampleUser(1L, "Alice");
        Document doc = sampleDocument(1L, "My Doc", owner);

        when(documentService.canAccess(1L, 1L)).thenReturn(true);
        when(documentService.getDocument(1L)).thenReturn(doc);

        mockMvc.perform(get("/api/documents/1")
                .session(authenticatedSession(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("My Doc"));
    }

    @Test
    @DisplayName("GET /api/documents/{id} – forbidden document returns 403")
    void getDocument_forbidden_returns403() throws Exception {
        when(documentService.canAccess(1L, 2L)).thenReturn(false);

        mockMvc.perform(get("/api/documents/1")
                .session(authenticatedSession(2L)))
            .andExpect(status().isForbidden());
    }

    // ── DELETE /api/documents/{id} ────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/documents/{id} – owner can delete their document")
    void deleteDocument_owner_returns200() throws Exception {
        doNothing().when(documentService).deleteDocument(1L, 1L);

        mockMvc.perform(delete("/api/documents/1")
                .session(authenticatedSession(1L)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Deleted"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} – non-owner gets 400 error")
    void deleteDocument_nonOwner_returns400() throws Exception {
        doThrow(new RuntimeException("Not the owner"))
            .when(documentService).deleteDocument(1L, 99L);

        mockMvc.perform(delete("/api/documents/1")
                .session(authenticatedSession(99L)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}
