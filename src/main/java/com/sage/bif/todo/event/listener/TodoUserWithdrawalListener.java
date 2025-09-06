package com.sage.bif.todo.event.listener;

import com.sage.bif.notification.repository.WebPushSubscriptionRepository;
import com.sage.bif.todo.repository.RoutineCompletionRepository;
import com.sage.bif.todo.repository.SubTodoCompletionRepository;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.user.event.model.UserWithdrawalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodoUserWithdrawalListener {

    private final TodoRepository todoRepository;
    private final SubTodoRepository subTodoRepository;
    private final RoutineCompletionRepository routineCompletionRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final SubTodoCompletionRepository subTodoCompletionRepository;

    @Order(1)
    @EventListener
    @Transactional
    public void handleUserWithdrawal(UserWithdrawalEvent event) {
        try {
            Long bifId = event.getBifId();

            int deletedRoutineCompletions = routineCompletionRepository.deleteByTodo_BifUser_BifId(bifId);

            int deletedSubTodoCompletions = subTodoCompletionRepository.deleteBySubTodo_Todo_BifUser_BifId(bifId);

            int deletedSubTodos = subTodoRepository.deleteByTodo_BifUser_BifId(bifId);

            int deletedRepeatDays = todoRepository.deleteRepeatDaysByBifId(bifId);

            int deletedTodos = todoRepository.deleteByBifUser_BifId(bifId);

            webPushSubscriptionRepository.deleteByBif_BifId(bifId);

            log.info("회원 탈퇴로 모든 사용자 데이터 삭제 완료: bifId={}, 할일={}개, 서브할일완료={}개, 서브할일={}개, 루틴완료기록={}개, 반복요일={}개, 웹푸시구독 삭제완료",
                    bifId, deletedTodos, deletedSubTodoCompletions, deletedSubTodos, deletedRoutineCompletions, deletedRepeatDays);

        } catch (Exception e) {
            log.error("회원 탈퇴 시 할 일 데이터 삭제 실패: bifId={}", event.getBifId(), e);
            throw e;
        }
    }

}
