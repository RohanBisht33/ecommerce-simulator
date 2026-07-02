package com.ecommerce.controller;

import com.ecommerce.entity.*;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
public class OrderController {
    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderController(OrderService orderService, ProductRepository productRepository,
                           CartService cartService, UserRepository userRepository, OrderRepository orderRepository, ProductService productService) {
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        // Ensure users can only access their own orders
        if (!order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "order-detail";
    }

    @GetMapping("/orders")
    public String showOrderHistory(Principal principal, Model model, @RequestParam(required = false) String status) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        List<Order> orders;

        if (status != null && !status.trim().isEmpty()) {
            orders = orderRepository.findByUserAndStatus(user, status.toUpperCase());
            model.addAttribute("currentStatus", status.toLowerCase());
        } else {
            orders = orderRepository.findByUser(user);
            model.addAttribute("currentStatus", "all");
        }

        // Pre-calculate the total spent to avoid issues parsing/evaluating inside the Thymeleaf template
        double totalSpent = orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                .sum();

        model.addAttribute("orders", orders);
        model.addAttribute("totalSpent", totalSpent);
        return "orders";
    }

    @GetMapping("/checkout")
    public String showOrderFront(Model model) {
        String checkoutToken = UUID.randomUUID().toString();
        model.addAttribute("checkoutToken", checkoutToken);
        model.addAttribute("orderItems", cartService.getCartItems());
        // Verify cart snapshot to detect concurrent modifications or tampering during checkout
        model.addAttribute("cartSnapshotString", cartService.getCartSnapshotSignature());

        BigDecimal subtotal = cartService.getSubTotal();
        calculateAndModelFinancials(subtotal, cartService.getTotalItemsCount(), model);

        return "checkout";
    }

    @PostMapping("/buy/now/{id}")
    public String processBuyNow(@PathVariable Long id, Model model) {
        String checkoutToken = UUID.randomUUID().toString();
        model.addAttribute("checkoutToken", checkoutToken);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID: " + id));

        var temporaryItem = new Object() {
            public Product getProduct() { return product; }
            public int getQuantity() { return 1; }
        };

        model.addAttribute("orderItems", Collections.singletonList(temporaryItem));

        BigDecimal subtotal = product.getPrice();
        calculateAndModelFinancials(subtotal, 1, model);

        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam("checkoutToken") String token,
                                  @RequestParam("cartSnapshot") String submittedSnapshot,
                                  @RequestParam(required = false) Long productId,
                                  @RequestParam(required = false, defaultValue = "1") Integer quantity,
                                  RedirectAttributes redirectAttributes) {
        Order completedOrder;

        try{
            if (productId == null) {
                String currentSessionSnapshot = cartService.getCartSnapshotSignature();

                if (!currentSessionSnapshot.equals(submittedSnapshot)) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Your cart was modified in another tab. Please review your total items and try again.");
                    return "redirect:/checkout";
                }
            }

            if (productId != null) {
                completedOrder = orderService.createOrderForSingleProduct(token, productId, quantity);
            } else {
                completedOrder = orderService.createOrderFromCart(token);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Order placed successfully! Tracking Number: " + completedOrder.getOrderNumber());
        }
        catch (DataIntegrityViolationException dive) {
            redirectAttributes.addFlashAttribute("errorMessage", "This order has already been processed.");

            return "redirect:/";
        }
        catch (ObjectOptimisticLockingFailureException oolfe) {
            // Handle concurrent cart updates during checkout
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart was modified in another tab. Please review your order totals and try again.");
        }

        return "redirect:/";
    }

    private void calculateAndModelFinancials(BigDecimal subtotal, int itemResetCount, Model model) {
        BigDecimal shippingCost = subtotal.compareTo(new BigDecimal("50")) >= 0 ? BigDecimal.ZERO : new BigDecimal("5.99");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05"));
        BigDecimal total = subtotal.add(shippingCost).add(tax);

        model.addAttribute("totalItems", itemResetCount);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("discount", BigDecimal.ZERO);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);
    }

    @Transactional
    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findOrderForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUser().getUsername().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
            return "redirect:/orders";
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending orders can be cancelled.");
            return "redirect:/orders";
        }

        order.setStatus(OrderStatus.CANCELLED);

        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = productRepository.findProductForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        orderRepository.save(order);
        redirectAttributes.addFlashAttribute("successMessage", "Order successfully cancelled and stock restored.");
        return "redirect:/orders";
    }

    @Transactional
    @PostMapping("/orders/refund/{id}")
    public String refundOrder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }

        Order order = orderRepository.findOrderForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUser().getUsername().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized action.");
            return "redirect:/orders";
        }

        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.DELIVERED) {
            redirectAttributes.addFlashAttribute("errorMessage", "This order is not eligible for a refund.");
            return "redirect:/orders";
        }

        order.setStatus(OrderStatus.REFUNDED);

        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = productRepository.findProductForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        orderRepository.save(order);
        redirectAttributes.addFlashAttribute("successMessage", "Refund requested successfully. Inventory updated.");
        return "redirect:/orders";
    }
}