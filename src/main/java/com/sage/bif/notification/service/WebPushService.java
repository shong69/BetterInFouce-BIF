package com.sage.bif.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.notification.dto.NotificationMessage;
import com.sage.bif.notification.dto.WebPushSubscriptionRequest;
import com.sage.bif.notification.entity.WebPushSubscription;
import com.sage.bif.notification.repository.WebPushSubscriptionRepository;
import com.sage.bif.todo.exception.UserNotFoundException;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final WebPushSubscriptionRepository subscriptionRepository;
    private final BifRepository bifRepository;
    private final ObjectMapper objectMapper;
    private final PushService pushService;

    @Transactional
    public void subscribe(Long bifId, WebPushSubscriptionRequest request) {
        if (bifId == null) {
            log.warn("WebPush 구독 실패: bifId가 null입니다.");
            return;
        }
        
        if (request == null || request.getEndpoint() == null) {
            log.warn("WebPush 구독 실패: 요청 데이터가 유효하지 않습니다. bifId={}", bifId);
            return;
        }

        Bif bif = bifRepository.findById(bifId)
                .orElseThrow(() -> new UserNotFoundException(bifId));

        subscriptionRepository.findByEndpoint(request.getEndpoint())
                .ifPresent(existing -> {
                    if (!existing.getBif().getBifId().equals(bifId)) {
                        log.warn("다른 사용자가 소유한 endpoint 재등록 시도: bifId={}", bifId);
                    }
                    subscriptionRepository.delete(existing);
                });

        WebPushSubscription subscription = WebPushSubscription.builder()
                .bif(bif)
                .endpoint(request.getEndpoint())
                .p256dh(request.getKeys().getP256dh())
                .auth(request.getKeys().getAuth())
                .build();

        subscriptionRepository.save(subscription);
    }

    public void sendNotification(Long bifId, NotificationMessage message) {
        if (bifId == null) {
            log.warn("WebPush 알림 전송 실패: bifId가 null입니다.");
            return;
        }
        
        if (message == null) {
            log.warn("WebPush 알림 전송 실패: message가 null입니다. bifId={}", bifId);
            return;
        }

        try {
            List<WebPushSubscription> subscriptions = subscriptionRepository.findAllByBif_BifId(bifId);

            if (subscriptions.isEmpty()) {
                return;
            }

            for (WebPushSubscription subscription : subscriptions) {
                sendToSubscription(bifId, message, subscription);
            }
        } catch (Exception e) {
            log.error("WebPush 알림 전송 중 예외 발생: bifId={}", bifId, e);
        }
    }

    @Transactional
    public void unsubscribe(Long bifId, String endpoint) {
        if (bifId == null || endpoint == null) {
            log.warn("WebPush 구독 해제 실패: 매개변수가 null입니다.");
            return;
        }
        
        subscriptionRepository.findByEndpoint(endpoint)
                .ifPresent(sub -> {
                    if (sub.getBif().getBifId().equals(bifId)) {
                        subscriptionRepository.delete(sub);
                    } else {
                        log.warn("무권한 구독 해제 시도: bifId={}가 다른 사용자의 endpoint 삭제 시도", bifId);
                    }
                });
    }

    @Transactional
    public void unsubscribeAll(Long bifId) {
        if (bifId == null) {
            log.warn("WebPush 전체 구독 해제 실패: bifId가 null입니다.");
            return;
        }
        
        subscriptionRepository.deleteByBif_BifId(bifId);
    }

    private void sendToSubscription(Long bifId, NotificationMessage message, WebPushSubscription subscription) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload
            );

            pushService.send(notification);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Web Push 알림 전송 중단됨: bifId={}, endpoint={}", 
                    bifId, subscription.getEndpoint(), e);
        } catch (Exception e) {
            log.error("Web Push 알림 전송 실패: bifId={}, endpoint={}",
                    bifId, subscription.getEndpoint(), e);

            if (isExpiredSubscription(e)) {
                subscriptionRepository.delete(subscription);
            }
        }
    }

    private boolean isExpiredSubscription(Exception e) {
        return e.getMessage() != null &&
                (e.getMessage().contains("410") || e.getMessage().contains("expired"));
    }

}
