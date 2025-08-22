package com.sage.bif.todo.event.listener;

import com.sage.bif.notification.repository.WebPushSubscriptionRepository;
import com.sage.bif.todo.repository.RoutineCompletionRepository;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.user.event.model.UserWithdrawalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawalListener {

    private final TodoRepository todoRepository;
    private final SubTodoRepository subTodoRepository;
    private final RoutineCompletionRepository routineCompletionRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    @EventListener
    @Transactional
    public void handleUserWithdrawal(UserWithdrawalEvent event) {
        try {
            Long bifId = event.getBifId();

            // 연관 테이블들을 순서대로 삭제 (외래키 제약 조건 고려)
            
            // 1. RoutineCompletion 삭제 (Todo와 연관)
            int deletedRoutineCompletions = routineCompletionRepository.deleteByTodo_BifUser_BifId(bifId);
            
            // 2. SubTodo 삭제 (Todo와 연관)  
            int deletedSubTodos = subTodoRepository.deleteByTodo_BifUser_BifId(bifId);
            
            // 3. todo_repeat_days 삭제 (Todo의 ElementCollection)
            // JPA에서 @ElementCollection은 부모 엔티티 삭제 시 자동 삭제되지만
            // 명시적으로 삭제하는 경우를 위해 네이티브 쿼리 사용
            int deletedRepeatDays = todoRepository.deleteRepeatDaysByBifId(bifId);
            
            // 4. Todo 엔티티 삭제 (메인 테이블)
            int deletedTodos = todoRepository.deleteByBifUser_BifId(bifId);
            
            // 5. WebPushSubscription 삭제 (Bif와 연관)
            webPushSubscriptionRepository.deleteByBif_BifId(bifId);

            log.info("회원 탈퇴로 모든 사용자 데이터 삭제 완료: bifId={}, 할일={}개, 서브할일={}개, 루틴완료기록={}개, 반복요일={}개, 웹푸시구독 삭제완료", 
                    bifId, deletedTodos, deletedSubTodos, deletedRoutineCompletions, deletedRepeatDays);

        } catch (Exception e) {
            log.error("회원 탈퇴 시 할 일 데이터 삭제 실패: bifId={}", event.getBifId(), e);
            throw e;
        }
    }

}
