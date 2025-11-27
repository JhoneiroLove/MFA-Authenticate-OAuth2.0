package com.security.mfaautenticate.service;

import com.security.mfaautenticate.entity.*;
import com.security.mfaautenticate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    // ==================== ROLE MANAGEMENT ====================

    @Transactional
    public Role createRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("Role already exists: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);

        Role saved = roleRepository.save(role);
        log.info("Created new role: {}", name);
        return saved;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        roleRepository.deleteById(roleId);
        log.info("Deleted role with id: {}", roleId);
    }

    // ==================== RESOURCE MANAGEMENT ====================

    @Transactional
    public Resource createResource(String name, String description, String path) {
        if (resourceRepository.existsByName(name)) {
            throw new RuntimeException("Resource already exists: " + name);
        }

        Resource resource = new Resource();
        resource.setName(name);
        resource.setDescription(description);
        resource.setPath(path);

        Resource saved = resourceRepository.save(resource);
        log.info("Created new resource: {} -> {}", name, path);
        return saved;
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Optional<Resource> getResourceByName(String name) {
        return resourceRepository.findByName(name);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        resourceRepository.deleteById(resourceId);
        log.info("Deleted resource with id: {}", resourceId);
    }

    // ==================== PERMISSION MANAGEMENT ====================

    @Transactional
    public Permission createPermission(Long resourceId, Operation operation) {
        Resource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));

        Optional<Permission> existing = permissionRepository.findByResourceAndOperation(resource, operation);
        if (existing.isPresent()) {
            throw new RuntimeException("Permission already exists for this resource and operation");
        }

        Permission permission = new Permission();
        permission.setResource(resource);
        permission.setOperation(operation);

        Permission saved = permissionRepository.save(permission);
        log.info("Created new permission: {} - {}", resource.getName(), operation);
        return saved;
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional
    public void deletePermission(Long permissionId) {
        permissionRepository.deleteById(permissionId);
        log.info("Deleted permission with id: {}", permissionId);
    }

    // ==================== ROLE-PERMISSION ASSIGNMENT ====================

    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

        role.getPermissions().add(permission);
        roleRepository.save(role);
        log.info("Assigned permission {} to role {}", permissionId, role.getName());
    }

    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

        role.getPermissions().remove(permission);
        roleRepository.save(role);
        log.info("Removed permission {} from role {}", permissionId, role.getName());
    }

    // ==================== USER-ROLE ASSIGNMENT ====================

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Assigned role {} to user {}", role.getName(), user.getEmail());
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        user.getRoles().remove(role);
        userRepository.save(user);
        log.info("Removed role {} from user {}", role.getName(), user.getEmail());
    }

    // ==================== PERMISSION CHECKING ====================

    public boolean hasPermission(String email, String resourcePath, Operation operation) {
        List<User> users = userRepository.findAllByEmail(email);
        if (users.isEmpty()) {
            return false;
        }

        Optional<Resource> resourceOpt = resourceRepository.findByPath(resourcePath);
        if (resourceOpt.isEmpty()) {
            return true; // If resource not registered, allow access
        }

        Resource resource = resourceOpt.get();

        for (User user : users) {
            for (Role role : user.getRoles()) {
                for (Permission permission : role.getPermissions()) {
                    if (permission.getResource().getId().equals(resource.getId()) &&
                        permission.getOperation().equals(operation)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isAdmin(String email) {
        List<User> users = userRepository.findAllByEmail(email);
        for (User user : users) {
            for (Role role : user.getRoles()) {
                if ("ADMIN".equals(role.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== INITIALIZATION ====================

    @Transactional
    public void initializeDefaultRoles() {
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrator with full access");
            roleRepository.save(adminRole);
            log.info("Created default ADMIN role");
        }

        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setDescription("Regular user with limited access");
            roleRepository.save(userRole);
            log.info("Created default USER role");
        }
    }
}
