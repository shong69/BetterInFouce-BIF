package com.sage.bif.common.jwt;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Note: JJWT 0.11.5 버전 사용으로 인해 deprecated API를 사용합니다.
 * 향후 JJWT 버전 업그레이드 시 최신 API로 변경 예정입니다.
 */
@SuppressWarnings({"deprecation", "java:S1134", "java:S5542"})
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:your-secret-key-here-make-it-long-enough-for-hs256}")
    private String jwtSecret;

    @Value("${jwt.access-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration:2592000000}")
    private long refreshTokenExpiration;

    public enum UserRole {
        BIF, GUARDIAN
    }

    public String generateAccessToken(String email, UserRole role, Long bifId, String nickname,
                                      String provider, String providerUniqueId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        claims.put("bifId", bifId);
        claims.put("nickname", nickname);
        claims.put("provider", provider);
        claims.put("providerUniqueId", providerUniqueId);

        return Jwts.builder()
                .setSubject(email)
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}