package com.example.warehouse.security;

import com.example.warehouse.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String required = resolveRequiredAccess(request);
        if (logger.isDebugEnabled()) {
            String principal = authentication == null ? "anonymous" : String.valueOf(authentication.getPrincipal());
            logger.debug("Access denied to {} for {} with authorities {}",
                    request.getRequestURI(),
                    principal,
                    authentication == null ? "[]" : authentication.getAuthorities());
            logger.debug("Required access for {}: {}", request.getRequestURI(), required);
        }

        ApiErrorResponse payload = new ApiErrorResponse("FORBIDDEN", "Access denied", required);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    private String resolveRequiredAccess(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/products")) {
            return "Requires role: ROLE_MANAGER or ROLE_STAFF";
        }
        if (uri.startsWith("/ws")) {
            return "Requires authentication";
        }
        return "Insufficient privileges";
    }
}
