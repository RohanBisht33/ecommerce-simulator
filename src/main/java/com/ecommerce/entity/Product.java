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
    @Min(value = 0, message = "Price cannot be empty")
    private BigDecimal price;

    @Column(nullable = false)
    @Min(value = 0, message = "Stock allocation cannot drop below zero")
    private Integer stock;

    public Product(){
    }
    public Product(Long id, String name, BigDecimal price, Integer stock){
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public Long getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public BigDecimal getPrice(){
        return price;
    }
    public Integer getStock(){
        return stock;
    }
    public void settId(Long id){
        this.id = id;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setPrice(BigDecimal price){
        this.price = price;
    }
    public void setStock(Integer stock){
        this.stock = stock;
    }
}
