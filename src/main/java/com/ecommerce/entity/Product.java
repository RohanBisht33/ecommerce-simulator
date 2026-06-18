package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Product name cannot be blank")
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false)
    @Min(value = 0, message = "Stock allocation cannot drop below zero")
    private Integer stock;

    @Column(nullable = false)
    private Integer stockCapacity = 100;

    // 🖼️ New Field: Stores the path or URL string of the product image
    @Column(length = 1000)
    private String imageUrl;

    private Integer discountPercent = 0; // Standard integer field initialized to 0% discount

    @NotBlank(message = "Category field cannot be empty")
    @Column(nullable = false)
    private String category;

    @NotBlank(message = "Product SKU cannot be blank")
    @Column(nullable = false, unique = true) // Enforces data integrity with a UNIQUE database constraint
    private String sku;

    public Product() {
    }

    public Product(Long id, String name, BigDecimal price, BigDecimal originalPrice, Integer stock, Integer stockCapacity, String imageUrl, Integer discountPercent, String category, String sku) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice; // Added
        this.stock = stock;
        this.stockCapacity = stockCapacity; // Added
        this.imageUrl = imageUrl;
        this.discountPercent = discountPercent;
        this.category = category;
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    // Getter for Thymeleaf rendering engine
    public String getImageUrl() {
        return imageUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    // ── New getters/setters ──────────────────────────────────────────────────

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getStockCapacity() {
        return stockCapacity;
    }

    public void setStockCapacity(Integer stockCapacity) {
        this.stockCapacity = stockCapacity;
    }
}