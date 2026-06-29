package com.ecommerce.controller;

import com.ecommerce.entity.*;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import org.springframework.http.HttpStatus;
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

    // 🔍 Display Itemized Order Receipt Sheet View
    @GetMapping("/orders/{id}")
    public String showOrderDetail(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        // 1. Fetch the order or fail if it doesn't exist
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        // 🛡️ Security Check: Ensure users can only view their own orders
        if (!order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders";
        }

        // 2. Inject dataset into the detail template scope
        model.addAttribute("order", order);
        return "order-detail";
    }
    
    // 📦 Display Secure User Order History Dashboard View
    @GetMapping("/orders")
    public String showOrderHistory(Principal principal, Model model, @RequestParam(required = false) String status) {
        if (principal == null) {
            return "redirect:/login";
        }

        // 1. Fetch the active user context profile from database
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        List<Order> orders;

        // 2. Process conditional filter matching based on chosen dashboard chip
        if (status != null && !status.trim().isEmpty()) {
            orders = orderRepository.findByUserAndStatus(user, status.toUpperCase());
            model.addAttribute("currentStatus", status.toLowerCase());
        } else {
            orders = orderRepository.findByUser(user);
            model.addAttribute("currentStatus", "all");
        }

        // 📊 3. Pre-calculate total spent on the backend to fix the Thymeleaf parsing crash
        double totalSpent = orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0)
                .sum();

        // 4. Inject datasets into Thymeleaf template scope
        model.addAttribute("orders", orders);
        model.addAttribute("totalSpent", totalSpent); // 🟢 Safely bound pre-calculated variable
        return "orders";
    }

    // 🛒 Standard Cart Checkout
    @GetMapping("/checkout")
    public String showOrderFront(Model model){
        model.addAttribute("orderItems", cartService.getCartItems());

        BigDecimal subtotal = cartService.getSubTotal();
        calculateAndModelFinancials(subtotal, cartService.getTotalItemsCount(), model);

        return "checkout";
    }

    // ⚡ Express "Buy Now" Flash Sale Instant Checkout
    @PostMapping("/buy/now/{id}")
    public String processBuyNow(@PathVariable Long id, Model model) {
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
    public String processCheckout(@RequestParam(required = false) Long productId,
                                  @RequestParam(required = false, defaultValue = "1") Integer quantity,
                                  RedirectAttributes redirectAttributes) {
        Order completedOrder;

        // 🟢 If a specific product ID is passed, process it as a single item buy now purchase
        if (productId != null) {
            completedOrder = orderService.createOrderForSingleProduct(productId, quantity);
        } else {
            // 🛒 Otherwise default back to checking out everything inside the active session cart
            completedOrder = orderService.createOrderFromCart();
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Order placed successfully! Tracking Number: " + completedOrder.getOrderNumber());

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