package com.security.mfaautenticate.repository;

import com.security.mfaautenticate.entity.Operation;
import com.security.mfaautenticate.entity.Permission;
import com.security.mfaautenticate.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByResource(Resource resource);
    Optional<Permission> findByResourceAndOperation(Resource resource, Operation operation);
}
