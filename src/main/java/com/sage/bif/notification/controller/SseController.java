package com.sage.bif.notification.controller;

import com.sage.bif.common.dto.CustomUserDetails;
import com.sage.bif.common.jwt.JwtTokenProvider;
import com.sage.bif.notification.exception.SseAuthenticationException;
import com.sage.bif.notification.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseNotificationService sseNotificationService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam(required = false) String token,
                                @CookieValue(value = "authenticatedUserToken", required = false) String authenticatedUserToken) {
        Long bifId = null;

        if (userDetails != null) {
            bifId = userDetails.getBifId();
        } else if (authenticatedUserToken != null && "SUCCESS".equals(jwtTokenProvider.validateToken(authenticatedUserToken))) {
            bifId = jwtTokenProvider.getBifIdFromToken(authenticatedUserToken);
        } else if (token != null && "SUCCESS".equals(jwtTokenProvider.validateToken(token))) {
            bifId = jwtTokenProvider.getBifIdFromToken(token);
        }

        if (bifId == null) {
            throw new SseAuthenticationException();
        }

        return sseNotificationService.subscribe(bifId);
    }

}
