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
        if (userRepository.findByUsername("admin").isEmpty()) {
            String rawPassword = System.getenv("ADMIN_INITIAL_PASSWORD");

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_INITIAL_PASSWORD environment variable must be set to create the initial admin user.");
            }

            String encryptedPassword = passwordEncoder.encode(rawPassword);
            User newUser = new User("admin", encryptedPassword, "ROLE_ADMIN");
            userRepository.save(newUser);
        }
    }
}
