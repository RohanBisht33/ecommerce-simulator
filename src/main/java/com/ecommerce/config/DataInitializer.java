package com.ecommerce.config;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String[] args) {
        // Fix: Pass "admin" for both parameters to match the method signature
        if (userRepository.findByUsernameOrEmail("admin", "admin").isEmpty()) {
            String rawPassword = System.getenv("ADMIN_INITIAL_PASSWORD");

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_INITIAL_PASSWORD environment variable must be set to create the initial admin user.");
            }

            String encryptedPassword = passwordEncoder.encode(rawPassword);
            // Default seed user gets identical username and email values for fallback testing
            User newUser = new User("admin", "admin@nexstore.com", encryptedPassword, "ROLE_ADMIN");
            userRepository.save(newUser);
        }
    }
}