package com.ecommerce.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ecommerce.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 🔍 Dynamic query method generated automatically by Spring Data JPA
    List<Product> findByCategory(String category);
}
