package com.security.mfaautenticate.controller;

import com.security.mfaautenticate.entity.User;
import com.security.mfaautenticate.repository.UserRepository;
import com.security.mfaautenticate.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("email", user.getEmail());
                    userMap.put("name", user.getName());
                    userMap.put("provider", user.getOauthProvider());
                    userMap.put("mfaEnabled", user.isMfaEnabled());
                    userMap.put("roles", user.getRoles().stream()
                            .map(role -> role.getName())
                            .collect(Collectors.toList()));
                    return userMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "message", "Lista de usuarios - Solo ADMIN",
                "admin", currentUser.getEmail(),
                "users", userList
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = userRepository.count();
        long usersWithMfa = userRepository.findAll().stream()
                .filter(User::isMfaEnabled)
                .count();

        return ResponseEntity.ok(Map.of(
                "message", "Estadísticas del sistema - Solo ADMIN",
                "totalUsers", totalUsers,
                "usersWithMfa", usersWithMfa
        ));
    }

    private User getUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUser();
        } else if (principal instanceof User) {
            return (User) principal;
        }

        throw new IllegalStateException("Tipo de principal no soportado: " + principal.getClass());
    }
}

