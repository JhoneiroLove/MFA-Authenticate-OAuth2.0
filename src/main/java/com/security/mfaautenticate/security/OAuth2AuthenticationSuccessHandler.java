package com.security.mfaautenticate.security;

import com.security.mfaautenticate.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        String targetUrl;

        if (user.isMfaEnabled()) {
            // Si tiene MFA habilitado, redirigir a verificación MFA
            targetUrl = UriComponentsBuilder.fromUriString("/mfa-verification.html")
                    .queryParam("email", user.getEmail())
                    .build().toUriString();
        } else {
            // Si no tiene MFA, generar token y redirigir al dashboard
            String token = tokenProvider.generateToken(user.getEmail());
            targetUrl = UriComponentsBuilder.fromUriString("/dashboard.html")
                    .queryParam("token", token)
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}