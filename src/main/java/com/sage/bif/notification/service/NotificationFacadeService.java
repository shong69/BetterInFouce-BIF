package com.sage.bif.notification.service;

import com.sage.bif.notification.dto.NotificationMessage;
import com.sage.bif.todo.entity.Todo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacadeService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final SseNotificationService sseNotificationService;
    private final WebPushService webPushService;

    public void sendNotificationByTodo(Todo todo) {
        if (todo == null || todo.getBifUser() == null) {
            log.warn("알림 전송 실패: Todo 또는 BifUser가 null입니다.");
            return;
        }

        NotificationMessage message = createTodoNotificationMessage(todo);
        Long bifId = todo.getBifUser().getBifId();

        if (bifId == null) {
            log.warn("알림 전송 실패: BifId가 null입니다. todoId={}", todo.getTodoId());
            return;
        }

        try {
            sseNotificationService.send(bifId, message);
        } catch (Exception e) {
            log.error("SSE 알림 전송 실패: bifId={}, todoId={}", bifId, todo.getTodoId(), e);
        }

        try {
            webPushService.sendNotification(bifId, message);
        } catch (Exception e) {
            log.error("WebPush 알림 전송 실패: bifId={}, todoId={}", bifId, todo.getTodoId(), e);
        }
    }

    private NotificationMessage createTodoNotificationMessage(Todo todo) {
        String body = createNotificationBody(todo);

        return NotificationMessage.builder()
                .title("할 일 알림")
                .body(body)
                .todoId(todo.getTodoId())
                .type("todo-reminder")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String createNotificationBody(Todo todo) {
        return todo.getTitle();
    }

}
