package com.sage.bif.diary.event.listener;

// import com.sage.bif.common.service.RedisService;
import com.sage.bif.diary.entity.Diary;
import com.sage.bif.diary.repository.AiFeedbackRepository;
import com.sage.bif.diary.repository.DiaryRepository;
import com.sage.bif.user.event.model.UserWithdrawalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component("diaryUserWithdrawalListener")
@RequiredArgsConstructor
public class UserWithdrawalListener {

    private final DiaryRepository diaryRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    // private final RedisService redisService;


    @Order(2)
    @EventListener
    @Transactional
    public void handleUserWithdrawal(UserWithdrawalEvent event) {
        try {
            log.info("사용자 탈퇴로 인한 diary 데이터 삭제 시작 - SocialId: {}, BifId: {}, Role: {}, EventId: {}",
                    event.getSocialId(), event.getBifId(), event.getUserRole(), event.getEventId());

            if (event.getUserRole() == com.sage.bif.common.jwt.JwtTokenProvider.UserRole.BIF && event.getBifId() != null) {
                deleteUserDiaryData(event.getBifId());
            }

            log.info("사용자 탈퇴로 인한 diary 데이터 삭제 완료 - SocialId: {}, BifId: {}, EventId: {}",
                    event.getSocialId(), event.getBifId(), event.getEventId());

        } catch (Exception e) {
            log.error("사용자 탈퇴로 인한 diary 데이터 삭제 중 오류 발생 - SocialId: {}, EventId: {}, Error: {}",
                    event.getSocialId(), event.getEventId(), e.getMessage(), e);
        }
    }

    private void deleteUserDiaryData(Long bifId) {
        try {
            var diaries = diaryRepository.findByUserId(bifId);
            log.info("삭제할 diary 수: {} - UserId: {}", diaries.size(), bifId);

            if (diaries.isEmpty()) {
                log.info("삭제할 diary가 없습니다 - UserId: {}", bifId);
                return;
            }

            List<Long> diaryIds = diaries.stream()
                    .map(Diary::getId)
                    .toList();

            int deletedAiFeedbacks = aiFeedbackRepository.deleteByDiaryIds(diaryIds);
            log.info("AI 피드백 일괄 삭제 완료 - 삭제된 수: {}", deletedAiFeedbacks);

            diaryRepository.deleteAll(diaries);
            log.info("사용자 diary 일괄 삭제 완료 - UserId: {}, 삭제된 diary 수: {}", bifId, diaries.size());

            // clearUserDiaryCache(bifId); // Redis 캐시 삭제 로직 주석 처리

        } catch (Exception e) {
            log.error("사용자 diary 데이터 삭제 중 오류 발생 - UserId: {}, Error: {}", bifId, e.getMessage(), e);
            throw e;
        }
    }

    // Redis 캐시 삭제 로직 주석 처리
    /*
    private void clearUserDiaryCache(Long bifId) {
        try {
            String cachePattern = "monthly_summary:" + bifId + ":*";
            boolean cacheDeleted = redisService.delete(cachePattern);
            log.info("사용자 diary 캐시 삭제 완료 - UserId: {}, 삭제 성공: {}", bifId, cacheDeleted);

        } catch (Exception e) {
            log.error("사용자 diary 캐시 삭제 중 오류 발생 - UserId: {}, Error: {}", bifId, e.getMessage(), e);
        }
    }
    */

}
