package com.sage.bif.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public final class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UserRole role, Long bifId, String nickname,
                                      String provider, String providerUniqueId, Long socialId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(providerUniqueId)
                .claim("role", role.name())
                .claim("bifId", bifId)
                .claim("nickname", nickname)
                .claim("provider", provider)
                .claim("socialId", socialId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String providerUniqueId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(providerUniqueId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String getProviderUniqueIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }

    public Long getBifIdFromToken(String token) {
        return getClaimsFromToken(token).get("bifId", Long.class);
    }

    public String getNicknameFromToken(String token) {
        return getClaimsFromToken(token).get("nickname", String.class);
    }

    public String getProviderFromToken(String token) {
        return getClaimsFromToken(token).get("provider", String.class);
    }

    public Long getSocialIdFromToken(String token) {
        return getClaimsFromToken(token).get("socialId", Long.class);
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return "SUCCESS";
        } catch (ExpiredJwtException e) {
            return "EXPIRED_TOKEN";
        } catch (JwtException e) {
            return "INVALID_TOKEN";
        } catch (Exception e) {
            return "SERVER_ERROR";
        }
    }

    public enum UserRole {
        BIF, GUARDIAN
    }

}
