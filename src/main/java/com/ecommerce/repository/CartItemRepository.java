package com.ecommerce.repository;

import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Eagerly fetch the product alongside each cart item in a single query.
    // Without this, product is a lazy proxy: fine while the Hibernate session
    // is open, but CartService reads item.getProduct().getPrice() after the
    // session that loaded it has already closed, causing
    // LazyInitializationException: could not initialize proxy - no Session.
    @EntityGraph(attributePaths = {"product"})
    List<CartItem> findByUser(User user);

    @EntityGraph(attributePaths = {"product"})
    Optional<CartItem> findByUserAndProductId(User user, Long productId);

    void deleteByUser(User user);
}