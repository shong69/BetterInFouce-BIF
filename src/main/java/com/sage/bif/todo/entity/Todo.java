package com.sage.bif.todo.entity;

import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bif_id")
    private User bifId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userInput;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String aiTitle;

    @Enumerated(EnumType.STRING)
    private TodoTypes type;

    @Enumerated(EnumType.STRING)
    private RepeatFrequency repeatFrequency;

    @Enumerated(EnumType.STRING)
    private RepeatDays repeatDays;

    @Column(columnDefinition = "DATE")
    private Date dueDate;

    @Column(columnDefinition = "TIME")
    private Time dueTime;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE" ,nullable = false)
    private Boolean notificationEnabled;

    @Column(columnDefinition = "INT DEFAULT 0", nullable = false)
    private Integer notificationTime;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    private Boolean isCompleted;

    @Column(columnDefinition = "DATETIME")
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE", nullable = false)
    private Boolean isDeleted;

} 