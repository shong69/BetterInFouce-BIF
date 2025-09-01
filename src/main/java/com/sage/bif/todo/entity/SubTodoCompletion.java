package com.sage.bif.todo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sub_todo_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"sub_todo_id", "completion_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTodoCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subTodoCompletionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_todo_id")
    private SubTodo subTodo;

    @Column(columnDefinition = "DATE", nullable = false)
    private LocalDate completionDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
