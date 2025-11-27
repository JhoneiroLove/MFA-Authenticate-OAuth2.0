package com.security.mfaautenticate.service;

import com.security.mfaautenticate.entity.OAuthProvider;
import com.security.mfaautenticate.entity.Role;
import com.security.mfaautenticate.entity.User;
import com.security.mfaautenticate.repository.RoleRepository;
import com.security.mfaautenticate.repository.UserRepository;
import com.security.mfaautenticate.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = OAuthProvider.valueOf(registrationId.toUpperCase());

        log.info("Login con provider: {}", provider);
        log.info("Atributos recibidos: {}", oauth2User.getAttributes());

        String email = extractEmail(oauth2User, provider);
        String oauthId = extractOAuthId(oauth2User, provider);
        String name = extractName(oauth2User, provider);

        log.info("Email extraído: {}", email);
        log.info("OAuth ID: {}", oauthId);
        log.info("Nombre: {}", name);

        // Buscar por provider + oauthId
        Optional<User> userOptional = userRepository.findByOauthProviderAndOauthId(provider, oauthId);

        User user;
        if (userOptional.isEmpty()) {
            // Usuario nuevo - verificar si hay otros usuarios con el mismo email
            List<User> existingUsers = userRepository.findAllByEmail(email);

            User.UserBuilder userBuilder = User.builder()
                    .email(email)
                    .name(name)
                    .oauthProvider(provider)
                    .oauthId(oauthId);

            // Si hay otros usuarios con el mismo email, heredar su configuración de MFA
            if (!existingUsers.isEmpty()) {
                User existingUser = existingUsers.get(0);
                log.info("Usuario existente encontrado con email: {}. Heredando configuración MFA.", email);

                userBuilder
                        .mfaEnabled(existingUser.isMfaEnabled())
                        .usingMfa(existingUser.isUsingMfa())
                        .mfaSecret(existingUser.getMfaSecret());
            } else {
                log.info("Nuevo usuario sin configuración MFA previa");
                userBuilder
                        .mfaEnabled(false)
                        .usingMfa(false);
            }

            user = userBuilder.build();
            user = userRepository.save(user);
            log.info("Usuario creado: {} con MFA: {}", user.getEmail(), user.isMfaEnabled());

            // Asignar rol ADMIN al primer usuario
            long totalUsers = userRepository.count();
            if (totalUsers == 1) {
                Optional<Role> adminRole = roleRepository.findByName("ADMIN");
                if (adminRole.isPresent()) {
                    user.getRoles().add(adminRole.get());
                    user = userRepository.save(user);
                    log.info("Primer usuario registrado - Rol ADMIN asignado a: {}", user.getEmail());
                } else {
                    log.warn("Rol ADMIN no encontrado en la base de datos");
                }
            }
        } else {
            user = userOptional.get();
            log.info("Usuario existente: {} con MFA: {}", user.getEmail(), user.isMfaEnabled());
        }

        return new CustomOAuth2User(oauth2User, user);
    }

    private String extractEmail(OAuth2User oauth2User, OAuthProvider provider) {
        String email = switch (provider) {
            case GOOGLE -> oauth2User.getAttribute("email");
            case GITHUB -> oauth2User.getAttribute("email");
            case FACEBOOK -> oauth2User.getAttribute("email");
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };

        // Fallback si no hay email
        if (email == null || email.isEmpty()) {
            String oauthId = extractOAuthId(oauth2User, provider);
            email = switch (provider) {
                case GITHUB -> {
                    String login = oauth2User.getAttribute("login");
                    yield (login != null ? login : oauthId) + "@github.com";
                }
                case FACEBOOK -> oauthId + "@facebook.com";
                default -> oauthId + "@oauth.com";
            };
            log.warn("Email no proporcionado por {}, usando fallback: {}", provider, email);
        }

        return email;
    }

    private String extractOAuthId(OAuth2User oauth2User, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> oauth2User.getAttribute("sub");
            case GITHUB -> {
                Object id = oauth2User.getAttribute("id");
                yield id != null ? id.toString() : null;
            }
            case FACEBOOK -> oauth2User.getAttribute("id");
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }

    private String extractName(OAuth2User oauth2User, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> oauth2User.getAttribute("name");
            case GITHUB -> {
                String nameAttr = oauth2User.getAttribute("name");
                if (nameAttr == null || nameAttr.isEmpty()) {
                    nameAttr = oauth2User.getAttribute("login");
                }
                yield nameAttr;
            }
            case FACEBOOK -> oauth2User.getAttribute("name");
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }
}