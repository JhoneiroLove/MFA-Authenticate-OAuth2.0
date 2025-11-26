package com.security.mfaautenticate.controller;

import com.security.mfaautenticate.dto.MfaVerificationRequest;
import com.security.mfaautenticate.entity.User;
import com.security.mfaautenticate.repository.UserRepository;
import com.security.mfaautenticate.security.JwtTokenProvider;
import com.security.mfaautenticate.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final MfaService mfaService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/mfa/setup")
    public ResponseEntity<?> setupMfa(@RequestParam String email) {
        try {
            log.info("Setup MFA para: {}", email);

            // Buscar todos los usuarios con este email
            List<User> users = userRepository.findAllByEmail(email);

            if (users.isEmpty()) {
                throw new RuntimeException("Usuario no encontrado");
            }

            String secret = mfaService.generateSecretKey();
            String qrCodeDataUri = mfaService.generateQRCodeDataUri(secret, email);

            // Guardar el secreto en TODOS los usuarios con este email
            for (User user : users) {
                user.setMfaSecret(secret);
                userRepository.save(user);
            }

            Map<String, String> response = new HashMap<>();
            response.put("secret", secret);
            response.put("qrCodeUrl", qrCodeDataUri);

            log.info("QR generado para: {}", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en setup MFA: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerificationRequest request) {
        try {
            log.info("Verificando MFA para: {}", request.getEmail());

            // Buscar todos los usuarios con este email
            List<User> users = userRepository.findAllByEmail(request.getEmail());

            if (users.isEmpty()) {
                throw new RuntimeException("Usuario no encontrado");
            }

            User firstUser = users.get(0);

            if (firstUser.getMfaSecret() == null) {
                throw new RuntimeException("MFA no configurado");
            }

            boolean isValid = mfaService.verifyCode(firstUser.getMfaSecret(), request.getCode());

            if (!isValid) {
                log.warn("Código MFA inválido para: {}", request.getEmail());
                return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
            }

            // Activar MFA en TODOS los usuarios con este email
            for (User user : users) {
                user.setMfaEnabled(true);
                user.setUsingMfa(true);
                userRepository.save(user);
            }

            // Generar nuevo token JWT
            String token = jwtTokenProvider.generateToken(firstUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "MFA activado correctamente");
            response.put("token", token);
            response.put("mfaEnabled", true);

            log.info("MFA activado exitosamente para: {}", request.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en verify MFA: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<?> disableMfa(@RequestParam String email) {
        try {
            log.info("Desactivando MFA para: {}", email);

            // Buscar todos los usuarios con este email
            List<User> users = userRepository.findAllByEmail(email);

            if (users.isEmpty()) {
                throw new RuntimeException("Usuario no encontrado");
            }

            // Desactivar MFA en TODOS los usuarios con este email
            for (User user : users) {
                user.setMfaEnabled(false);
                user.setUsingMfa(false);
                user.setMfaSecret(null);
                userRepository.save(user);
            }

            log.info("MFA desactivado para: {}", email);

            return ResponseEntity.ok(Map.of("message", "MFA desactivado correctamente"));
        } catch (Exception e) {
            log.error("Error en disable MFA: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<?> getMfaStatus(@RequestParam String email) {
        try {
            log.info("Consultando estado MFA para: {}", email);

            // Buscar todos los usuarios con este email
            List<User> users = userRepository.findAllByEmail(email);

            if (users.isEmpty()) {
                throw new RuntimeException("Usuario no encontrado");
            }

            // Tomar el primer usuario (todos comparten el mismo estado de MFA)
            User user = users.get(0);

            Map<String, Object> response = new HashMap<>();
            response.put("mfaEnabled", user.isMfaEnabled());
            response.put("email", user.getEmail());
            response.put("name", user.getName());

            log.info("Estado MFA para {}: {}", email, user.isMfaEnabled());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al obtener estado MFA para {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}