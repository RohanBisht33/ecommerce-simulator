package com.ecommerce.controller;

import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CartController {
    private final CartService cartService;
    private final ProductRepository productRepository;

    public CartController(CartService cartService, ProductRepository productRepository) {
        this.cartService = cartService;
        this.productRepository = productRepository;
    }

    @GetMapping("/cart")
    public String showCartFront(Model model){
        model.addAttribute("cartItems", cartService.getCartItems());

        BigDecimal subtotal = cartService.getSubTotal();
        BigDecimal shippingCost = subtotal.compareTo(new BigDecimal("50")) >= 0 ? BigDecimal.ZERO : new BigDecimal("5.99");
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05"));
        BigDecimal total = subtotal.add(shippingCost).add(tax);

        model.addAttribute("totalItems", cartService.getTotalItemsCount());
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("discount", BigDecimal.ZERO); // Placeholder default value
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/update/{cartItemId}")
    public String updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestParam String action) {

        if ("increase".equals(action)) {
            cartService.increaseQuantity(cartItemId);
        } else if ("decrease".equals(action)) {
            cartService.decreaseQuantity(cartItemId);
        }

        return "redirect:/cart";
    }
}
