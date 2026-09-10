package com.example.warehouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import com.example.warehouse.security.JwtAuthenticationFilter;
import com.example.warehouse.security.RestAccessDeniedHandler;
import com.example.warehouse.security.RestAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    @Value("${wms.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/categories/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/customers/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/suppliers/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/suppliers/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/suppliers/**").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/sales-orders/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/sales-orders/*/ship").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/sales-orders/*/confirm").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/sales-orders/*/cancel").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/sales-orders").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/purchase-orders/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/purchase-orders/*/receive").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/purchase-orders/*/order").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/purchase-orders/*/cancel").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/purchase-orders").hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/inventory/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/inventory/adjustments").hasAnyRole("MANAGER", "STAFF")

                        .requestMatchers(HttpMethod.GET, "/api/communications/**").hasAnyRole("MANAGER", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/audit-log/**").hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasRole("MANAGER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> cleanOrigins = allowedOrigins.stream()
                .flatMap(origins -> Arrays.stream(origins.split(",")))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        configuration.setAllowedOriginPatterns(cleanOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "If-Match", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
