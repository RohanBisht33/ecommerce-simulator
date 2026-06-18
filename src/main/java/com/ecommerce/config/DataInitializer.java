package com.ecommerce.config;

import com.ecommerce.entity.User;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 🔐 Inject the security context encoder

    public DataInitializer(ProductRepository productRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Product> productList = new ArrayList<>();

        // 🟢 Securely seed encrypted users
        if (userRepository.count() == 0) {
            userRepository.save(new User("alex", passwordEncoder.encode("password123"), "CUSTOMER"));
            userRepository.save(new User("admin", passwordEncoder.encode("admin123"), "ADMIN"));
            System.out.println("🔐 Mock user identities securely encoded and seeded successfully!");
        }

        if (productRepository.count() == 0) {
            Product keyboard = new Product(
                    null,
                    "Quantum Mechanical Keyboard",
                    new BigDecimal("129.99"),
                    new BigDecimal("150.00"),
                    45,
                    100,
                    "https://imgs.search.brave.com/G3V9fnCGNMYEtS7WPp-UPSPPPM6yStMl7Ga1hnLl1_w/rs:fit:500:0:1:0/g:ce/aHR0cHM6Ly93d3cu/cmVkcmFnb24uaW4v/Y2RuL3Nob3AvZmls/ZXMvTWFpbl9mMzg3/YTY4Ni02MjQ3LTQ3/OTYtOGQyZS1mMDJk/NGRjNTkyZWQucG5n/P3Y9MTc3MDQ1Njgz/NCZ3aWR0aD0xMDAw",
                    20,
                    "Electronics",
                    "NEX-KB-001"
            );
            productList.add(keyboard);

            Product mouse = new Product(
                    null,
                    "Ergonomic Vertical Mouse",
                    new BigDecimal("150.00"),
                    new BigDecimal("150.00"),
                    20,
                    100,
                    "https://imgs.search.brave.com/uW6omaaRianUZSkP8EhkQCypXoWPAO9VhSEh7LRUaLc/rs:fit:500:0:1:0/g:ce/aHR0cHM6Ly9tLm1l/ZGlhLWFtYXpvbi5j/b20vaW1hZ2VzL0kv/NTFhanFhZFR0YUwu/anBn",
                    0,
                    "Electronics",
                    "NEX-MS-002"
            );
            productList.add(mouse);

            Product monitor = new Product(
                    null,
                    "UltraWide Curved Monitor 34\"",
                    new BigDecimal("81.00"),
                    new BigDecimal("100.00"),
                    10,
                    100,
                    "https://imgs.search.brave.com/2eKI0Y0EQ1eoRMf8jgvtaDP4K551HNI0q1qRpZPOCVg/rs:fit:500:0:1:0/g:ce/aHR0cHM6Ly9pbWFn/ZXMtbmEuc3NsLWlt/YWdlcy1hbWF6b24u/Y29tL2ltYWdlcy9J/LzcxTlpKajB5d09M/LmpwZw",
                    19,
                    "Electronics",
                    "NEX-MN-003"
            );
            productList.add(monitor);

            Product headphones = new Product(
                    null,
                    "Noise Cancelling Studio Headphones",
                    new BigDecimal("249.00"),
                    new BigDecimal("249.00"),
                    0,
                    100,
                    "https://imgs.search.brave.com/xtjskQ2djj3S4lm9QhDA5Qdi4UTG8FhZXdTUWOhMcXw/rs:fit:500:0:1:0/g:ce/aHR0cHM6Ly9tLm1l/ZGlhLWFtYXpvbi5j/b20vaW1hZ2VzL0kv/MzFqOEN0WXJnWUwu/anBn",
                    0,
                    "Electronics",
                    "NEX-HP-004"
            );
            productList.add(headphones);

            productRepository.saveAll(productList);
            System.out.println("💾 Local Database successfully initialized with mock e-commerce products!");
        }
    }
}