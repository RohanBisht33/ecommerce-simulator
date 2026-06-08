package com.ecommerce.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ecommerce.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{
    
}
