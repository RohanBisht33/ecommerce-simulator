package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    public OrderController(OrderService orderService, ProductRepository productRepository,
                           CartService cartService, UserRepository userRepository, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
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

        // 3. Inject datasets into Thymeleaf template scope
        model.addAttribute("orders", orders);
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
    public String processCheckout(RedirectAttributes redirectAttributes) {
        Order completedOrder = orderService.createOrder();

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
}