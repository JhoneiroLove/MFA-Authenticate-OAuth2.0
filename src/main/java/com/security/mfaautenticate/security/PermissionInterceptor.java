package com.security.mfaautenticate.security;

import com.security.mfaautenticate.service.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionInterceptor implements HandlerInterceptor {

    private final RbacService rbacService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }

        if (annotation == null) {
            return true;
        }

        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            log.warn("No authenticated user for protected resource");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return false;
        }

        String email = principal.getName();

        // Admins tienen acceso a todo
        if (rbacService.isAdmin(email)) {
            log.debug("Admin user {} accessing {}", email, annotation.resource());
            return true;
        }

        boolean hasPermission = rbacService.hasPermission(email, annotation.resource(), annotation.operation());

        if (!hasPermission) {
            log.warn("User {} lacks permission {} on {}", email, annotation.operation(), annotation.resource());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
            return false;
        }

        log.debug("User {} has permission {} on {}", email, annotation.operation(), annotation.resource());
        return true;
    }
}
