package com.sage.bif.simulation.event.listener;

import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.simulation.repository.SimulationRecommendationRepository;
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
public class SimulationUserWithdrawalListener {

    private final SimulationRecommendationRepository simulationRecommendationRepository;

    @Order(3)
    @EventListener
    @Transactional
    public void handleUserWithdrawal(UserWithdrawalEvent event) {
        try {
            if (event.getUserRole() == com.sage.bif.common.jwt.JwtTokenProvider.UserRole.BIF && event.getBifId() != null) {
                simulationRecommendationRepository.deleteByBif_BifId(event.getBifId());
            }

            if (event.getUserRole() == JwtTokenProvider.UserRole.GUARDIAN && event.getSocialId() != null) {
                simulationRecommendationRepository.deleteByGuardianSocialId(event.getSocialId());
            }

        } catch (Exception e) {
            log.error("사용자 탈퇴로 인한 simulation 데이터 삭제 중 오류 발생 - SocialId: {}, EventId: {}",
                    event.getSocialId(), event.getEventId(), e);
        }
    }

}
