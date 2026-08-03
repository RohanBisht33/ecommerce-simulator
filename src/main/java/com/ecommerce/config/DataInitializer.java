package com.ecommerce.config;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_INITIAL_PASSWORD:${admin.initial-password:admin123}}")
    private String adminInitialPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String[] args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
                logger.warn("ADMIN_INITIAL_PASSWORD is not set. Skipping initial admin user creation.");
                return;
            }

            logger.info("Initializing admin user...");
            String encryptedPassword = passwordEncoder.encode(adminInitialPassword);
            User newUser = new User("admin", encryptedPassword, "ROLE_ADMIN", "admin@nexstore.com");
            userRepository.save(newUser);
            logger.info("Admin user created successfully.");
        }
    }
}
