package com.example.warehouse.security;

import com.example.warehouse.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    public static final String JWT_FAILURE_ATTRIBUTE = "jwt.failure.reason";
    public static final String JWT_FILTER_RAN_ATTRIBUTE = "jwt.filter.ran";
    public static final String JWT_HEADER_PRESENT_ATTRIBUTE = "jwt.header.present";
    public static final String JWT_TOKEN_EXTRACTED_ATTRIBUTE = "jwt.token.extracted";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        request.setAttribute(JWT_FILTER_RAN_ATTRIBUTE, Boolean.TRUE);
        request.setAttribute(JWT_HEADER_PRESENT_ATTRIBUTE, hasAuthorizationHeader(request));

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);
            request.setAttribute(JWT_TOKEN_EXTRACTED_ATTRIBUTE, token != null);
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
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Authenticated {} with authorities {}",
                                    claims.getSubject(), authorities);
                        }
                    } else {
                        request.setAttribute(JWT_FAILURE_ATTRIBUTE, "Token expired");
                        if (logger.isDebugEnabled()) {
                            logger.debug("JWT token expired for {}", request.getRequestURI());
                        }
                    }
                } catch (Exception ex) {
                    // Invalid token; fall through to security handling.
                    request.setAttribute(JWT_FAILURE_ATTRIBUTE, ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    if (logger.isDebugEnabled()) {
                        logger.debug("JWT validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
                    }
                }
            } else if (isProtectedRequest(request)) {
                request.setAttribute(JWT_FAILURE_ATTRIBUTE, "Missing bearer token");
                if (logger.isDebugEnabled()) {
                    logger.debug("No JWT token resolved for protected endpoint {}", request.getRequestURI());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = findAuthorizationHeader(request);
        if (header != null) {
            String trimmed = header.trim();
            if (trimmed.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length == 2 && !parts[1].isBlank()) {
                    return parts[1].trim();
                }
            }
        }
        String queryToken = request.getParameter("access_token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }
        return null;
    }

    private String findAuthorizationHeader(HttpServletRequest request) {
        String value = request.getHeader("Authorization");
        if (value != null && !value.isBlank()) {
            return value;
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if ("authorization".equalsIgnoreCase(name)) {
                String candidate = request.getHeader(name);
                if (candidate != null && !candidate.isBlank()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean hasAuthorizationHeader(HttpServletRequest request) {
        return findAuthorizationHeader(request) != null;
    }

    private boolean isProtectedRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/") || uri.startsWith("/ws/");
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
