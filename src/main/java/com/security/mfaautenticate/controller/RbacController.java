package com.security.mfaautenticate.controller;

import com.security.mfaautenticate.entity.*;
import com.security.mfaautenticate.repository.UserRepository;
import com.security.mfaautenticate.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rbac")
@RequiredArgsConstructor
@Slf4j
public class RbacController {

    private final RbacService rbacService;
    private final UserRepository userRepository;

    // ==================== ROLE ENDPOINTS ====================

    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            Role role = rbacService.createRole(name, description);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error("Error creating role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(rbacService.getAllRoles());
    }

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<?> deleteRole(@PathVariable Long roleId) {
        try {
            rbacService.deleteRole(roleId);
            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== RESOURCE ENDPOINTS ====================

    @PostMapping("/resources")
    public ResponseEntity<?> createResource(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String path = request.get("path");
            Resource resource = rbacService.createResource(name, description, path);
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            log.error("Error creating resource", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/resources")
    public ResponseEntity<List<Resource>> getAllResources() {
        return ResponseEntity.ok(rbacService.getAllResources());
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<?> deleteResource(@PathVariable Long resourceId) {
        try {
            rbacService.deleteResource(resourceId);
            return ResponseEntity.ok(Map.of("message", "Resource deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting resource", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== PERMISSION ENDPOINTS ====================

    @PostMapping("/permissions")
    public ResponseEntity<?> createPermission(@RequestBody Map<String, Object> request) {
        try {
            Long resourceId = Long.valueOf(request.get("resourceId").toString());
            Operation operation = Operation.valueOf(request.get("operation").toString());
            Permission permission = rbacService.createPermission(resourceId, operation);
            return ResponseEntity.ok(permission);
        } catch (Exception e) {
            log.error("Error creating permission", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(rbacService.getAllPermissions());
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<?> deletePermission(@PathVariable Long permissionId) {
        try {
            rbacService.deletePermission(permissionId);
            return ResponseEntity.ok(Map.of("message", "Permission deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting permission", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== ASSIGNMENT ENDPOINTS ====================

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<?> assignPermissionToRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        try {
            rbacService.assignPermissionToRole(roleId, permissionId);
            return ResponseEntity.ok(Map.of("message", "Permission assigned to role successfully"));
        } catch (Exception e) {
            log.error("Error assigning permission to role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<?> removePermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        try {
            rbacService.removePermissionFromRole(roleId, permissionId);
            return ResponseEntity.ok(Map.of("message", "Permission removed from role successfully"));
        } catch (Exception e) {
            log.error("Error removing permission from role", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        try {
            rbacService.assignRoleToUser(userId, roleId);
            return ResponseEntity.ok(Map.of("message", "Role assigned to user successfully"));
        } catch (Exception e) {
            log.error("Error assigning role to user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<?> removeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable Long roleId) {
        try {
            rbacService.removeRoleFromUser(userId, roleId);
            return ResponseEntity.ok(Map.of("message", "Role removed from user successfully"));
        } catch (Exception e) {
            log.error("Error removing role from user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== USER ENDPOINTS ====================

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserWithRoles(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("roles", user.getRoles());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== OPERATIONS ENUM ====================

    @GetMapping("/operations")
    public ResponseEntity<Operation[]> getOperations() {
        return ResponseEntity.ok(Operation.values());
    }
}
