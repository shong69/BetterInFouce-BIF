package com.sage.bif.todo.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.user.entity.Bif;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "todos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long todoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bif_id", nullable = false)
    private Bif bifUser;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Size(max = 1000, message = "사용자 입력은 1000자를 초과할 수 없습니다")
    private String userInput;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 1, max = 255, message = "제목은 1-255자 사이여야 합니다")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TodoTypes type;

    @Enumerated(EnumType.STRING)
    private RepeatFrequency repeatFrequency;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "todo_repeat_days", joinColumns = @JoinColumn(name = "todo_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_day")
    private List<RepeatDays> repeatDays;

    @Column(columnDefinition = "DATE")
    private LocalDate dueDate;

    @Column(columnDefinition = "TIME")
    private LocalTime dueTime;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE", nullable = false)
    @Builder.Default
    private Boolean notificationEnabled = true;

    @Column(columnDefinition = "INT DEFAULT 0", nullable = false)
    @Builder.Default
    private Integer notificationTime = 0;

    @Column
    private LocalDateTime lastNotificationSentAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "INT DEFAULT 0", nullable = false)
    @Builder.Default
    private Integer currentStep = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<SubTodo> subTodos = new ArrayList<>();

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

}
