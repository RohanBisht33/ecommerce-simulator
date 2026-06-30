package com.ecommerce.controller;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.AzureBlobStorageService;
import com.ecommerce.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AzureBlobStorageService blobStorageService;
    private final ProductService productService;

    public AdminController(ProductRepository productRepository, OrderRepository orderRepository,
                           AzureBlobStorageService blobStorageService, ProductService productService){
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.blobStorageService = blobStorageService;
        this.productService = productService;
    }

    @GetMapping("/orders")
    public String showAdminOrders(Model model){

        List<Order> allOrders = orderRepository.findAll();
        model.addAttribute("orders", allOrders);

        return "admin-orders";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetails(Model model, @PathVariable Long id){

        Order order = orderRepository.findById(id).orElseThrow(()-> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));

        model.addAttribute("order", order);

        return "order-detail";
    }
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("status") String newStatusParam,
                                    RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusParam.toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unknown status: " + newStatusParam);
            return "redirect:/admin/orders/" + id;
        }

        // 📦 Decrement stock only on the PENDING -> PROCESSING transition (admin confirmation).
        // This is intentionally NOT done anywhere else, so stock can't move until an admin
        // has actually reviewed and confirmed the order. The whole order is deducted in one
        // transaction, so a single out-of-stock item rolls back any earlier deductions too.
        if (oldStatus == OrderStatus.PENDING && newStatus == OrderStatus.PROCESSING) {
            try {
                productService.deductStockForOrder(order);
            } catch (RuntimeException ex) {
                // Stock for one or more items ran out before this admin confirmed it —
                // nothing was deducted (transaction rolled back), order stays PENDING for review.
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Could not confirm order: " + ex.getMessage());
                return "redirect:/admin/orders/" + id;
            }
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("successMessage",
                "Order " + order.getOrderNumber() + " updated to " + newStatus + ".");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/products")
    public String showAdminProducts(Model model){
        List<Product> allProducts = productRepository.findAll();

        model.addAttribute("products", allProducts);

        return "admin-products";
    }

    @PostMapping("/products/add")
    public String saveProduct(Product product, @RequestPart("imageFile") MultipartFile imageFile) {
        try {
            // 1. Stream the file binary data directly to Azure Blob Storage
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = blobStorageService.uploadImage(imageFile);

                // 2. Assign the permanent cloud URL to our domain model
                product.setImageUrl(imageUrl);
            } else if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
                // Case 2: A text URL path was manually typed into the form input
                product.setImageUrl(product.getImageUrl());
            }

            // 3. Commit the updated product record into PostgreSQL
            productRepository.save(product);

        } catch (IOException e) {
            // In production, log this error securely and handle the failure boundary gracefully
            return "redirect:/admin/products?error=upload_failed";
        }

        return "redirect:/admin/products";
    }

    @Transactional
    @PostMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id,
                              @ModelAttribute Product formProduct,
                              @RequestParam("oldStock") int oldFormStock,
                              @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        try {
            Product existingProduct = productRepository.findProductForUpdate(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

            // 2. Safely merge basic data details
            existingProduct.setName(formProduct.getName());
            existingProduct.setPrice(formProduct.getPrice());
            existingProduct.setOriginalPrice(formProduct.getOriginalPrice());
            existingProduct.setCategory(formProduct.getCategory());
            existingProduct.setDiscountPercent(formProduct.getDiscountPercent());
            existingProduct.setStockCapacity(formProduct.getStockCapacity());

            // 3. Concurrency-Safe Stock Logic: Update the inventory allocation explicitly
            int delta = formProduct.getStock() - oldFormStock;
            existingProduct.setStock(existingProduct.getStock() + delta);

            if (imageFile != null && !imageFile.isEmpty()) {
                // Case 1: New file uploaded — push to Azure and update existing record
                String imageUrl = blobStorageService.uploadImage(imageFile);
                existingProduct.setImageUrl(imageUrl);
            } else if (formProduct.getImageUrl() != null && !formProduct.getImageUrl().isBlank()) {
                // Case 2: A text URL path was manually typed into the form input
                existingProduct.setImageUrl(formProduct.getImageUrl());
            }
            productRepository.save(existingProduct);

        } catch (IOException e) {
            return "redirect:/admin/products?error=upload_failed";
        }

        return "redirect:/admin/products";
    }
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id){
        productRepository.deleteById(id);

        return "redirect:/admin/products";
    }
}