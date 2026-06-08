package com.ecommerce.controller;

import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {
    private final ProductRepository productRepository;
    private final CartService cartService;

    public ProductController(ProductRepository productRepository, CartService cartService){
        this.productRepository = productRepository;
        this.cartService = cartService;
    }
    @GetMapping("/")
    public String showStorefront(Model model) {

        model.addAttribute("products", productRepository.findAll());
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
    public String addToCart(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // 1. Fetch product
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 2. Add to cart service (which you already defined in CartService)
        cartService.addProduct(product);

        // 3. Feedback
        redirectAttributes.addFlashAttribute("successMessage", product.getName() + " added to cart!");

        return "redirect:/"; // Stays on the main index page
    }
}
