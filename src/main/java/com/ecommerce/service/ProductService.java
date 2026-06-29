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

    public ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    @Transactional
    public Product verifyAndDeductStock(Long id, Integer quantity){
        Product product = productRepository.findProductForUpdate(id).orElseThrow(()-> new RuntimeException("Product not found"));

        if(product.getStock() < quantity){
            throw new IllegalArgumentException("Insufficient inventory allocation available for product: " + product.getName());
        }
        product.setStock(product.getStock()-quantity);
        return productRepository.save(product);
    }

    // 🛡️ Atomic, all-or-nothing stock deduction for every line item in an order.
    // If any single item is out of stock, the whole method throws and Spring rolls
    // back every deduction made earlier in the loop within this same transaction —
    // so a failed confirmation never leaves the order half-deducted.
    @Transactional
    public void deductStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            verifyAndDeductStock(item.getProduct().getId(), item.getQuantity());
        }
    }
}