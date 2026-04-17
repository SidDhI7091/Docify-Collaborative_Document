package com.docify.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "task_assignees",
    uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "user_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskAssignee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}