package com.sage.bif.notification.controller;

import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.notification.dto.WebPushSubscriptionRequest;
import com.sage.bif.notification.service.WebPushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/web-push")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid @RequestBody WebPushSubscriptionRequest request) {
        Long bifId = userDetails.getBifId();
        
        webPushService.subscribe(bifId, request);
        return ResponseEntity.ok("Web Push 구독이 완료되었습니다.");
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @RequestParam String endpoint) {
        Long bifId = userDetails.getBifId();
        
        webPushService.unsubscribe(bifId, endpoint);
        return ResponseEntity.ok("Web Push 구독이 해제되었습니다.");
    }

    @DeleteMapping("/unsubscribe-all")
    public ResponseEntity<String> unsubscribeAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long bifId = userDetails.getBifId();
        
        webPushService.unsubscribeAll(bifId);
        return ResponseEntity.ok("모든 Web Push 구독이 해제되었습니다.");
    }

}
