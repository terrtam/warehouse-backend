package com.example.warehouse.security;

import com.example.warehouse.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token != null) {
                try {
                    Claims claims = jwtUtil.parseClaims(token);
                    if (jwtUtil.isTokenValid(claims)) {
                        List<GrantedAuthority> authorities = jwtUtil.extractRoles(claims)
                                .stream()
                                .map(this::normalizeRole)
                                .distinct()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                        accessor.setUser(authentication);
                        if (logger.isDebugEnabled()) {
                            logger.debug("WebSocket CONNECT authenticated {} with authorities {}",
                                    claims.getSubject(), authorities);
                        }
                    }
                } catch (Exception ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("WebSocket CONNECT JWT validation failed");
                    }
                }
            }
        }
        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        String accessToken = accessor.getFirstNativeHeader("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }
        return null;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return role;
        }
        String trimmed = role.trim();
        String upper = trimmed.toUpperCase(Locale.ENGLISH);
        if (upper.startsWith("ROLE_")) {
            return upper;
        }
        return "ROLE_" + upper;
    }
}
