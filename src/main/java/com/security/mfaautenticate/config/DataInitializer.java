package com.security.mfaautenticate.config;

import com.security.mfaautenticate.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RbacService rbacService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing default roles...");
        rbacService.initializeDefaultRoles();
        log.info("Default roles initialized successfully");
    }
}
