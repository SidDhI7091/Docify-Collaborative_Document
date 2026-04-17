package com.docify.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "notifications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // recipient

    @Column(nullable = false)
    private String type; // TASK_ASSIGNED | TASK_COMPLETED

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "is_read")
    private boolean read;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }
}
