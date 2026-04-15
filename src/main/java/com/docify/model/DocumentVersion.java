package com.docify.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "document_versions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id")
    private DocumentTab tab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_by")
    private User savedBy;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "version_name")
    private String versionName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }
}