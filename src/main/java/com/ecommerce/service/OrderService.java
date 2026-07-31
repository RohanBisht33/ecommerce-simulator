package com.ecommerce.service;

import com.ecommerce.entity.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.OrderFinancials;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        UserRepository userRepository, CartService cartService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.productService = productService;
    }

    @Transactional
    public Order createOrderFromCart(String token) {
        rejectDuplicateToken(token);

        User user = getCurrentAuthenticatedUser();
        BigDecimal subtotal = cartService.getSubTotal();

        for (CartItem cartItem : cartService.getCartItems()) {
            productService.verifyStockAvailable(
                    cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(OrderFinancials.calculateTotal(subtotal));
        order.setUser(user);
        order.setIdempotencyKey(token);

        for (CartItem cartItem : cartService.getCartItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setOrder(order);

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart();
        return savedOrder;
    }

    @Transactional
    public Order createOrderForSingleProduct(String token, Long productId, int quantity) {
        rejectDuplicateToken(token);

        User user = getCurrentAuthenticatedUser();
        productService.verifyStockAvailable(productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setTotalAmount(OrderFinancials.calculateTotal(subtotal));
        order.setIdempotencyKey(token);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getPrice());
        orderItem.setOrder(order);

        order.getOrderItems().add(orderItem);

        return orderRepository.save(order);
    }

    private void rejectDuplicateToken(String token) {
        if (orderRepository.findByIdempotencyKey(token).isPresent()) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                    "Order with idempotency key already exists");
        }
    }

    private User getCurrentAuthenticatedUser() {
        // 1. Extract the credential string used to log into the application
        String currentUsernameOrEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Query both columns simultaneously using the single identifier
        return userRepository.findByUsernameOrEmail(currentUsernameOrEmail, currentUsernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User profile not found for: " + currentUsernameOrEmail));
    }
}
