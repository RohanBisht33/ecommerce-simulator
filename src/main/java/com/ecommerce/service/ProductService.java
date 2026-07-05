package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product verifyStockAvailable(Long id, int requiredQuantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStock() < requiredQuantity) {
            throw new IllegalArgumentException(
                    "Insufficient inventory available for product: " + product.getName());
        }
        return product;
    }

    @Transactional
    public Product deductStock(Long id, Integer quantity) {
        Product product = productRepository.findProductForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStock() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient inventory allocation available for product: " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }

    @Transactional
    public void deductStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            deductStock(item.getProduct().getId(), item.getQuantity());
        }
    }

    @Transactional
    public void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findProductForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }
}
