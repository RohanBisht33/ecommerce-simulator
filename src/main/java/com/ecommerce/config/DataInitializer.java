package com.ecommerce.config;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String[] args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            // Create default admin user using password from environment, or generate a random one-time password.
            String rawPassword = System.getenv("ADMIN_INITIAL_PASSWORD");

            if (rawPassword == null || rawPassword.isBlank()) {
                rawPassword = generateRandomPassword();
                log.warn("No ADMIN_INITIAL_PASSWORD environment variable was set. " +
                        "Generated a random initial admin password: {}. " +
                        "Log in and change it immediately; this will not be shown again.", rawPassword);
            }

            String encryptedPassword = passwordEncoder.encode(rawPassword);

            User newUser = new User("admin", encryptedPassword, "ROLE_ADMIN");
            userRepository.save(newUser);
        }
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}