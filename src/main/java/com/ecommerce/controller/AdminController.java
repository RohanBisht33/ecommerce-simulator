package com.ecommerce.controller;

import com.ecommerce.entity.*;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.AzureBlobStorageService;
import com.ecommerce.service.OrderStatusService;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final AzureBlobStorageService blobStorageService;
    private final ProductService productService;
    private final OrderStatusService orderStatusService;

    public AdminController(ProductRepository productRepository, OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           AzureBlobStorageService blobStorageService, ProductService productService,
                           OrderStatusService orderStatusService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.blobStorageService = blobStorageService;
        this.productService = productService;
        this.orderStatusService = orderStatusService;
    }

    @GetMapping("/orders")
    public String showAdminOrders(Model model,
                                  @RequestParam(required = false) String status) {

        List<Order> allOrders = orderRepository.findAll();

        List<Order> filteredOrders;
        if (status != null && !status.isBlank()) {
            String upperStatus = status.toUpperCase();
            filteredOrders = allOrders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().name().equals(upperStatus))
                    .toList();
            model.addAttribute("currentStatus", status.toLowerCase());
        } else {
            filteredOrders = allOrders;
            model.addAttribute("currentStatus", "all");
        }

        long pendingCount = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();

        BigDecimal pendingValue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        long confirmedToday = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PROCESSING
                        && o.getOrderDate() != null
                        && o.getOrderDate().toLocalDate().equals(today))
                .count();

        long cancelledToday = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED
                        && o.getOrderDate() != null
                        && o.getOrderDate().toLocalDate().equals(today))
                .count();

        model.addAttribute("orders", filteredOrders);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("pendingValue", pendingValue);
        model.addAttribute("confirmedToday", confirmedToday);
        model.addAttribute("cancelledToday", cancelledToday);

        return "admin-orders";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetails(Model model, @PathVariable Long id) {

        Order order = orderRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found"));

        model.addAttribute("order", order);
        model.addAttribute("isAdminView", true);
        model.addAttribute("allStatuses", OrderStatus.values());

        return "order-detail";
    }

    @Transactional
    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("status") String newStatusParam,
                                    RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findOrderForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusParam.toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unknown status: " + newStatusParam);
            return "redirect:/admin/orders/" + id;
        }

        if (!orderStatusService.isTransitionAllowed(oldStatus, newStatus)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot change order status from " + oldStatus + " to " + newStatus + ".");
            return "redirect:/admin/orders/" + id;
        }

        if (orderStatusService.requiresStockDeduction(oldStatus, newStatus)) {
            try {
                productService.deductStockForOrder(order);
            } catch (RuntimeException ex) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Could not confirm order: " + ex.getMessage());
                return "redirect:/admin/orders/" + id;
            }
        }

        if (orderStatusService.requiresStockRestoration(oldStatus, newStatus)) {
            productService.restoreStockForOrder(order);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("successMessage",
                "Order " + order.getOrderNumber() + " updated to " + newStatus + ".");
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/products")
    public String showAdminProducts(Model model,
                                    @RequestParam(required = false) String category) {
        List<Product> allProducts = productRepository.findAll();

        List<Product> products;
        if (category != null && !category.isBlank()) {
            products = productRepository.findByCategory(category);
            model.addAttribute("currentCategory", category);
        } else {
            products = allProducts;
            model.addAttribute("currentCategory", "");
        }

        long totalProducts = allProducts.size();
        long inStockCount = allProducts.stream().filter(p -> p.getStock() > 5).count();
        long lowStockCount = allProducts.stream()
                .filter(p -> p.getStock() > 0 && p.getStock() <= 5)
                .count();
        long outOfStockCount = allProducts.stream().filter(p -> p.getStock() <= 0).count();

        model.addAttribute("products", products);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);

        return "admin-products";
    }

    @PostMapping("/products/add")
    public String saveProduct(@Valid Product product,
                              BindingResult bindingResult,
                              @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/products";
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = blobStorageService.uploadImage(imageFile);
                product.setImageUrl(imageUrl);
            } else if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
                product.setImageUrl(product.getImageUrl());
            }
            productRepository.save(product);
        } catch (IOException e) {
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

            existingProduct.setName(formProduct.getName());
            existingProduct.setPrice(formProduct.getPrice());
            existingProduct.setOriginalPrice(formProduct.getOriginalPrice());
            existingProduct.setCategory(formProduct.getCategory());
            existingProduct.setDiscountPercent(formProduct.getDiscountPercent());
            existingProduct.setStockCapacity(formProduct.getStockCapacity());

            int delta = formProduct.getStock() - oldFormStock;
            int newStock = existingProduct.getStock() + delta;
            if (newStock < 0) {
                return "redirect:/admin/products?error=stock_conflict";
            }
            existingProduct.setStock(newStock);

            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = blobStorageService.uploadImage(imageFile);
                existingProduct.setImageUrl(imageUrl);
            } else if (formProduct.getImageUrl() != null && !formProduct.getImageUrl().isBlank()) {
                existingProduct.setImageUrl(formProduct.getImageUrl());
            }
            productRepository.save(existingProduct);

        } catch (IOException e) {
            return "redirect:/admin/products?error=upload_failed";
        }

        return "redirect:/admin/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (orderItemRepository.existsByProduct_Id(id)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete product: it is referenced by existing orders.");
            return "redirect:/admin/products";
        }

        productRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        return "redirect:/admin/products";
    }
}
