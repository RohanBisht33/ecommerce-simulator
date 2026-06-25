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
    public void run(String []args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            String encryptedPassword = passwordEncoder.encode("admin");

            User newUser = new User("admin", encryptedPassword , "ROLE_ADMIN");
            userRepository.save(newUser);
        }
    }
}