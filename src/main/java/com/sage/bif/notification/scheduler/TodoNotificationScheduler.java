package com.sage.bif.notification.scheduler;

import com.sage.bif.notification.service.NotificationFacadeService;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoNotificationScheduler {

    private static final int NOTIFICATION_TIME_RANGE_MINUTES = 1;
    private static final int ROUTINE_TIME_TOLERANCE_MINUTES = 1;
    private final TodoRepository todoRepository;
    private final NotificationFacadeService notificationFacadeService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void checkAndSendTodoNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        try {
            List<Todo> todosToNotify = todoRepository.findTodosForNotification(today);

            if (todosToNotify.isEmpty()) {
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("알림 대상 할 일 개수: {} 건", todosToNotify.size());
            }

            List<Todo> filteredTodos = todosToNotify.stream()
                    .filter(todo -> shouldSendNotification(todo, now))
                    .toList();

            if (filteredTodos.isEmpty()) {
                return;
            }

            log.info("실제 알림 전송 대상: {} 건", filteredTodos.size());

            filteredTodos.forEach(todo -> processNotification(todo, now));

        } catch (RuntimeException e) {
            log.error("알림 스케줄러 실행 중 오류 발생", e);
        }
    }

    private void processNotification(Todo todo, LocalDateTime now) {
        try {
            CompletableFuture.runAsync(() -> sendNotificationAsync(todo))
                    .exceptionally(throwable -> {
                        log.error("알림 전송 비동기 처리 실패: todoId={}", todo.getTodoId(), throwable);
                        return null;
                    });

            updateNotificationStatusSafely(todo, now);

            log.info("할 일 알림 전송 완료: todoId={}, bifId={}, title={}",
                    todo.getTodoId(), todo.getBifUser().getBifId(), todo.getTitle());

        } catch (RuntimeException e) {
            log.error("알림 처리 중 예외 발생: todoId={}", todo.getTodoId(), e);
        }
    }

    private void sendNotificationAsync(Todo todo) {
        try {
            notificationFacadeService.sendNotificationByTodo(todo);
            if (log.isDebugEnabled()) {
                log.debug("알림 전송 성공: todoId={}", todo.getTodoId());
            }
        } catch (RuntimeException e) {
            log.error("알림 전송 실패: todoId={}, title={}", todo.getTodoId(), todo.getTitle(), e);
        }
    }

    private void updateNotificationStatusSafely(Todo todo, LocalDateTime sentAt) {
        try {
            Todo managedTodo = todoRepository.findById(todo.getTodoId()).orElse(null);
            if (managedTodo != null) {
                managedTodo.setLastNotificationSentAt(sentAt);
                todoRepository.save(managedTodo);
                if (log.isDebugEnabled()) {
                    log.debug("알림 전송 시간 기록 성공: todoId={}, sentAt={}", todo.getTodoId(), sentAt);
                }
            } else {
                log.warn("Todo를 찾을 수 없어 알림 시간 기록 실패: todoId={}", todo.getTodoId());
            }
        } catch (RuntimeException e) {
            log.error("알림 상태 업데이트 실패: todoId={}", todo.getTodoId(), e);
        }
    }

    private boolean shouldSendNotification(Todo todo, LocalDateTime currentDateTime) {
        if (!isBasicValidationPassed(todo)) {
            if (log.isDebugEnabled()) {
                log.debug("기본 검증 실패: todoId={}, enabled={}, completed={}, deleted={}, dueTime={}", 
                        todo.getTodoId(), todo.getNotificationEnabled(), todo.getIsCompleted(), 
                        todo.getIsDeleted(), todo.getDueTime());
            }
            return false;
        }

        if (todo.getType() == TodoTypes.ROUTINE && !isValidDayForRoutine(todo, currentDateTime.toLocalDate())) {
            return false;
        }

        LocalDateTime notificationDateTime = calculateNotificationDateTime(todo, currentDateTime);
        if (notificationDateTime == null) {
            log.warn("알림 시간 계산 실패: todoId={}", todo.getTodoId());
            return false;
        }

        if (hasRecentNotificationSent(todo, notificationDateTime)) {
            if (log.isDebugEnabled()) {
                log.debug("최근 알림 전송됨: todoId={}, lastSent={}", todo.getTodoId(), todo.getLastNotificationSentAt());
            }
            return false;
        }

        boolean inRange = isInNotificationTimeRange(currentDateTime, notificationDateTime);
        if (!inRange && log.isDebugEnabled()) {
            log.debug("알림 시간 범위 밖: todoId={}, 현재시간={}, 알림시간={}", 
                    todo.getTodoId(), currentDateTime, notificationDateTime);
        }

        return inRange;
    }

    private boolean isBasicValidationPassed(Todo todo) {
        return todo.getNotificationEnabled() && 
               !todo.getIsCompleted() && 
               !todo.getIsDeleted() && 
               todo.getDueTime() != null;
    }

    private LocalDateTime calculateNotificationDateTime(Todo todo, LocalDateTime currentDateTime) {
        try {
            LocalDate targetDate = todo.getType() == TodoTypes.ROUTINE ?
                    currentDateTime.toLocalDate() : todo.getDueDate();

            if (targetDate == null) {
                return null;
            }

            LocalDateTime dueDateTime = LocalDateTime.of(targetDate, todo.getDueTime());

            return dueDateTime.minusMinutes(todo.getNotificationTime());
        } catch (RuntimeException e) {
            log.error("알림 시간 계산 실패: todoId={}", todo.getTodoId(), e);
            return null;
        }
    }

    private boolean isInNotificationTimeRange(LocalDateTime currentDateTime, LocalDateTime notificationDateTime) {
        return currentDateTime.isAfter(notificationDateTime.minusMinutes(NOTIFICATION_TIME_RANGE_MINUTES)) &&
                currentDateTime.isBefore(notificationDateTime.plusMinutes(NOTIFICATION_TIME_RANGE_MINUTES));
    }

    private boolean hasRecentNotificationSent(Todo todo, LocalDateTime scheduledNotificationTime) {
        if (todo.getLastNotificationSentAt() == null) {
            return false;
        }

        LocalDateTime lastSent = todo.getLastNotificationSentAt();

        if (todo.getType() == TodoTypes.ROUTINE) {
            return lastSent.toLocalDate().equals(scheduledNotificationTime.toLocalDate()) &&
                    lastSent.getHour() == scheduledNotificationTime.getHour() &&
                    Math.abs(lastSent.getMinute() - scheduledNotificationTime.getMinute()) <= ROUTINE_TIME_TOLERANCE_MINUTES;
        }

        return lastSent.isAfter(scheduledNotificationTime.minusMinutes(NOTIFICATION_TIME_RANGE_MINUTES)) &&
                lastSent.isBefore(scheduledNotificationTime.plusMinutes(NOTIFICATION_TIME_RANGE_MINUTES));
    }

    private boolean isValidDayForRoutine(Todo todo, LocalDate date) {
        if (todo.getRepeatDays() == null || todo.getRepeatDays().isEmpty()) {
            log.warn("루틴 할 일에 repeatDays가 설정되지 않음: todoId={}, title={}", 
                    todo.getTodoId(), todo.getTitle());
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        RepeatDays targetDay = convertToRepeatDay(dayOfWeek);

        boolean isValidDay = todo.getRepeatDays().contains(targetDay);
        
        if (log.isDebugEnabled()) {
            log.debug("루틴 요일 검증: todoId={}, 오늘={}, 설정된요일={}, 결과={}", 
                    todo.getTodoId(), targetDay, todo.getRepeatDays(), isValidDay);
        }

        return isValidDay;
    }

    private RepeatDays convertToRepeatDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> RepeatDays.MONDAY;
            case TUESDAY -> RepeatDays.TUESDAY;
            case WEDNESDAY -> RepeatDays.WEDNESDAY;
            case THURSDAY -> RepeatDays.THURSDAY;
            case FRIDAY -> RepeatDays.FRIDAY;
            case SATURDAY -> RepeatDays.SATURDAY;
            case SUNDAY -> RepeatDays.SUNDAY;
        };
    }

}
