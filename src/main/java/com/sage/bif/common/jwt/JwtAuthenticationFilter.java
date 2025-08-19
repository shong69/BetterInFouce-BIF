package com.sage.bif.common.jwt;

import com.sage.bif.common.dto.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt)) {
            String result = jwtTokenProvider.validateToken(jwt);
            if (result.equals("SUCCESS")) {
                Long bifId = jwtTokenProvider.getBifIdFromToken(jwt);
                String nickname = jwtTokenProvider.getNicknameFromToken(jwt);
                String provider = jwtTokenProvider.getProviderFromToken(jwt);
                String providerUniqueId = jwtTokenProvider.getProviderUniqueIdFromToken(jwt);
                String roleString = jwtTokenProvider.getRoleFromToken(jwt);
                if (roleString == null) {
                    return;
                }
                JwtTokenProvider.UserRole role = JwtTokenProvider.UserRole.valueOf(roleString);
                Long socialId = jwtTokenProvider.getSocialIdFromToken(jwt);

                CustomUserDetails userDetails = new CustomUserDetails(jwt, bifId, nickname, provider, providerUniqueId, role, socialId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                String jsonResponse = String.format(
                        "{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                        result, result, java.time.LocalDateTime.now()
                );
                response.getWriter().write(jsonResponse);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
