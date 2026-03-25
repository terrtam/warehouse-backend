package com.example.warehouse.controller;

import com.example.warehouse.entity.User;
import com.example.warehouse.service.AuthAuditService;
import com.example.warehouse.service.AuthService;
import com.example.warehouse.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthAuditService authAuditService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        try {
            User user = authService.authenticate(
                    username,
                    body.get("password")
            );

            String token = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getRole()
            );

            authAuditService.logLogin(user.getUsername(), true, "Login successful");
            return ResponseEntity.ok(Map.of("token", token));

        } catch (RuntimeException e) {
            authAuditService.logLogin(username, false, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }
    }
}
