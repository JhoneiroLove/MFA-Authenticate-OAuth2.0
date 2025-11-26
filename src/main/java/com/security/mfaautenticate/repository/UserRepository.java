package com.security.mfaautenticate.repository;

import com.security.mfaautenticate.entity.OAuthProvider;
import com.security.mfaautenticate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findAllByEmail(String email);

    Optional<User> findByOauthProviderAndOauthId(OAuthProvider provider, String oauthId);
}