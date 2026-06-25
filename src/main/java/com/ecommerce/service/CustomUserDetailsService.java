package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service // ⚙️ This tells Spring to automatically inject this into the SecurityFilterChain
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Fetch our custom entity from the PostgreSQL/H2 database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found in database"));

        // 2. Translate it into Spring Security's required UserDetails format
        // Your DB stores "ROLE_CUSTOMER" and "ROLE_ADMIN" — use authorities() which takes the value as-is
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())  // "ROLE_CUSTOMER" stays "ROLE_CUSTOMER" ✓
                .build();    }
}