package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Use EntityGraph to fetch orderItems and products in a single JOIN query,
    // avoiding N+1 select performance issues when rendering order lists.

    @Override
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findAll();

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findByUser(User user);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findByUserAndStatus(User user, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findOrderForUpdate(@Param("id") Long id);

}