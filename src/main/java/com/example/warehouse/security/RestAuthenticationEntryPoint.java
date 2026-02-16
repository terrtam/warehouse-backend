package com.example.warehouse.security;

import com.example.warehouse.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String jwtFailure = (String) request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE);
        Boolean filterRan = (Boolean) request.getAttribute(JwtAuthenticationFilter.JWT_FILTER_RAN_ATTRIBUTE);
        Boolean headerPresentFromFilter = (Boolean) request.getAttribute(JwtAuthenticationFilter.JWT_HEADER_PRESENT_ATTRIBUTE);
        Boolean tokenExtracted = (Boolean) request.getAttribute(JwtAuthenticationFilter.JWT_TOKEN_EXTRACTED_ATTRIBUTE);
        String details = jwtFailure == null || jwtFailure.isBlank() ? null : jwtFailure;
        if (details == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                details = "Missing bearer token";
            } else if (!Boolean.TRUE.equals(filterRan)) {
                details = "Authorization header present but JWT filter did not run";
            } else if (Boolean.FALSE.equals(tokenExtracted)) {
                details = "Authorization header present but token extraction failed";
            } else if (Boolean.FALSE.equals(headerPresentFromFilter)) {
                details = "Authorization header present in entry point but not visible in JWT filter";
            } else {
                details = "Authorization header present but authentication was not established";
            }
        }
        details = details + " [diag filterRan=" + filterRan
                + ", headerSeenInFilter=" + headerPresentFromFilter
                + ", tokenExtracted=" + tokenExtracted + "]";
        if (logger.isDebugEnabled()) {
            logger.debug("Unauthorized request to {}: {}, jwtFailure={}",
                    request.getRequestURI(), authException.getMessage(), details);
        }

        ApiErrorResponse payload = new ApiErrorResponse("UNAUTHORIZED", "Authentication required", details);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
