package com.ecommerce.controller;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationPage() {
        return "register";
    }

    @PostMapping("/register")
    public String handleUserRegistration(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);
        model.addAttribute("email", email);
        model.addAttribute("username", username);

        if (!password.equals(confirmPassword)) {
            model.addAttribute("registerError", "Passwords do not match!");
            model.addAttribute("confirmError", "Password confirmation mismatch.");
            return "register";
        }

        if (password.length() < 8) {
            model.addAttribute("registerError", "Registration failed.");
            model.addAttribute("passwordError", "Password must be at least 8 characters.");
            return "register";
        }
    
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            model.addAttribute("registerError", "Registration failed.");
            model.addAttribute("usernameError", "Username is already taken!");
            return "register";
        }

        try {
            String encryptedPassword = passwordEncoder.encode(password);
            User newUser = new User(username, encryptedPassword, "ROLE_CUSTOMER", email);
            userRepository.save(newUser);

            redirectAttributes.addFlashAttribute("registerSuccess", "Account created successfully! Please sign in.");
            return "redirect:/login";

        } catch (DataIntegrityViolationException dive) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username or email is already taken!");
            return "redirect:/register";
        }
    }
}