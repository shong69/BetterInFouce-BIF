package com.sage.bif.notification.service;

import com.sage.bif.notification.dto.NotificationMessage;
import com.sage.bif.notification.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseNotificationService {

    private static final Long SSE_TIMEOUT_MINUTES = 60L * 1000 * 60;
    private final EmitterRepository emitterRepository;

    public SseEmitter subscribe(Long userId) {
        if (userId == null) {
            log.warn("SSE 구독 실패: userId가 null입니다.");
            throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        }

        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(SSE_TIMEOUT_MINUTES));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
        emitter.onError(e -> {
            log.error("SSE 연결 에러: emitterId={}", emitterId, e);
            emitterRepository.deleteById(emitterId);
        });

        sendToClient(emitter, emitterId, "connect", "SSE connection successful. (emitterId=" + emitterId + ")");

        Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByUserId(String.valueOf(userId));
        events.forEach((key, value) -> sendToClient(emitter, key, "notification", value));

        emitterRepository.deleteAllEventCacheStartWithId(String.valueOf(userId));

        return emitter;
    }

    public void send(Long userId, NotificationMessage message) {
        if (userId == null) {
            log.warn("SSE 알림 전송 실패: userId가 null입니다.");
            return;
        }

        if (message == null) {
            log.warn("SSE 알림 전송 실패: message가 null입니다. userId={}", userId);
            return;
        }

        try {
            Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(String.valueOf(userId));

            if (emitters.isEmpty()) {
                log.warn("SSE 알림 전송 실패: 해당 사용자의 활성 연결이 없습니다. userId={}", userId);
                return;
            }

            log.info("SSE 알림 전송 시작: userId={}, 활성연결수={}, 메시지={}", userId, emitters.size(), message.getTitle());

            emitters.forEach((key, emitter) -> {
                emitterRepository.saveEventCache(key, message);
                sendToClient(emitter, key, "notification", message);
            });
        } catch (Exception e) {
            log.error("SSE 알림 전송 중 예외 발생: userId={}", userId, e);
        }
    }

    private void sendToClient(SseEmitter emitter, String emitterId, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            log.error("SSE 이벤트 전송 실패: emitterId={}, eventName={}. 연결을 삭제합니다.",
                    emitterId, eventName, exception);
            emitterRepository.deleteById(emitterId);
        }
    }

}
