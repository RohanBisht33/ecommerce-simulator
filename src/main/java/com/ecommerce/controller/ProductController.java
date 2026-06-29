package com.ecommerce.controller;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import com.ecommerce.service.ProductService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class ProductController {
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final ProductService productService;


    public ProductController(ProductRepository productRepository, CartService cartService, ProductService productService){
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.productService = productService;
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

    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @RequestParam(required = false) String redirectUrl,
                            @RequestParam(required = false) Boolean buyNow,
                            RedirectAttributes redirectAttributes) {
        try {
            // Concurrency Safe Deduction using Pessimistic Write Lock
            Product product = productService.verifyAndDeductStock(id, quantity);

            for (int i = 0; i < quantity; i++) {
                cartService.addProduct(product);
            }

            redirectAttributes.addFlashAttribute("successMessage", product.getName() + " added to cart!");

            if (Boolean.TRUE.equals(buyNow)) {
                return "redirect:/cart";
            }

            if (redirectUrl != null && !redirectUrl.isBlank()) {
                return "redirect:" + redirectUrl;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (redirectUrl != null && !redirectUrl.isBlank()) {
                return "redirect:" + redirectUrl;
            }
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
