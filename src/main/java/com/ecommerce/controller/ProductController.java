package com.ecommerce.controller;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;

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

import java.util.ArrayList;
import java.util.List;

@Controller
public class ProductController {
    private final ProductRepository productRepository;
    private final CartService cartService;

    public ProductController(ProductRepository productRepository, CartService cartService){
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    @GetMapping("/")
    public String showStorefront(Model model, @RequestParam(required = false) String category) {

        if (category == null || category.trim().isEmpty()) {
            model.addAttribute("products", productRepository.findAll());
        }
        else {
            model.addAttribute("products", productRepository.findByCategory(category));
        }
        return "index";
    }

    @Transactional
    @PostMapping("/warehouse/restock/{id}")
    public String restockProduct(@PathVariable Long id, @RequestParam Integer quantity){
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found! "));
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
        return "redirect:/";
    }

    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @RequestParam(required = false) String redirectUrl,
                            @RequestParam(required = false) Boolean buyNow,
                            RedirectAttributes redirectAttributes) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Add the explicitly designated item quantities into the user session cart
        for (int i = 0; i < quantity; i++) {
            cartService.addProduct(product);
        }

        redirectAttributes.addFlashAttribute("successMessage", product.getName() + " added to cart!");

        // FIX #6: If Buy Now wrapper flag is triggered, intercept and route straight to cart layout screen
        if (Boolean.TRUE.equals(buyNow)) {
            return "redirect:/cart";
        }

        // FIX #3: If context path contains a custom redirect string, return to it instead of the home screen index fallback
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("product", product);
        return "product-detail";
    }
}
