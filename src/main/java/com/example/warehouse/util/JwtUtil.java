package com.example.warehouse.util;

import com.example.warehouse.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs
    ) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, UserRole role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null || expiration.after(new Date());
    }

    public List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<>();
        addRolesFromClaim(roles, claims.get("role"));
        addRolesFromClaim(roles, claims.get("roles"));
        addRolesFromClaim(roles, claims.get("authorities"));
        return roles;
    }

    private void addRolesFromClaim(List<String> roles, Object claimValue) {
        if (claimValue == null) {
            return;
        }
        if (claimValue instanceof String roleValue) {
            if (!roleValue.isBlank()) {
                roles.add(roleValue);
            }
            return;
        }
        if (claimValue instanceof Collection<?> roleValues) {
            for (Object value : roleValues) {
                if (value instanceof String roleValue && !roleValue.isBlank()) {
                    roles.add(roleValue);
                } else if (value instanceof Map<?, ?> roleMap) {
                    Object authority = roleMap.get("authority");
                    if (authority instanceof String roleValue && !roleValue.isBlank()) {
                        roles.add(roleValue);
                    }
                }
            }
        }
    }
}
