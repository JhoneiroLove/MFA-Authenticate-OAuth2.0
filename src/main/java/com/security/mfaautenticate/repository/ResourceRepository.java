package com.security.mfaautenticate.repository;

import com.security.mfaautenticate.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByName(String name);
    Optional<Resource> findByPath(String path);
    boolean existsByName(String name);
}
