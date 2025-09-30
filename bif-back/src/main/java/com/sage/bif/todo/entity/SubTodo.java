package com.sage.bif.todo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sub_todos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subTodoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id")
    @JsonBackReference
    private Todo todo;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String title;

    @Column(columnDefinition = "INT DEFAULT 0", nullable = false)
    private Integer sortOrder;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

}
