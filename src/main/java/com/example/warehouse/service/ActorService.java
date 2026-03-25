package com.example.warehouse.service;

import com.example.warehouse.entity.User;
import com.example.warehouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ActorService {

    @Autowired
    private UserRepository userRepository;

    public Actor getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? "system" : String.valueOf(authentication.getName());
        Optional<User> user = userRepository.findByUsername(username);
        return new Actor(user.map(User::getId).orElse(null), username);
    }

    public static class Actor {
        private final UUID userId;
        private final String username;

        public Actor(UUID userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public UUID getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
