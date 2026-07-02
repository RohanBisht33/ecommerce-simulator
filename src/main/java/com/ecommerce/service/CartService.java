package com.ecommerce.service;

import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import java.math.BigDecimal;
import java.util.*;

@Service
@SessionScope
public class CartService {

    private final Map<Long, CartItem> items = new HashMap<>();

    // Since CartService is session-scoped, synchronizing on this local lock
    // prevents concurrent modification races within the same user session.
    // Note: This won't work across multiple nodes without sticky sessions;
    // a distributed store (e.g., Redis) would be needed for that.
    private final Object lock = new Object();

    public Collection<CartItem> getCartItems(){
        synchronized (lock) {
            return new ArrayList<>(items.values());
        }
    }

    public void addProduct(Product product){
        synchronized (lock) {
            CartItem existingItem = items.get(product.getId());

            if (existingItem == null) {
                items.put(product.getId(), new CartItem(product.getId(), product, 1));
            } else {
                existingItem.setQuantity(existingItem.getQuantity() + 1);
            }
        }
    }

    public void increaseQuantity(Long id) {
        synchronized (lock) {
            CartItem existingItem = items.get(id);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + 1);
            }
        }
    }

    public void decreaseQuantity(Long id) {
        synchronized (lock) {
            CartItem existingItem = items.get(id);

            if (existingItem != null) {
                // Decrement first and remove the item if quantity drops to 0 or below,
                // avoiding a bug where items with quantity 1 were left in the cart.
                int newQuantity = existingItem.getQuantity() - 1;
                if (newQuantity <= 0) {
                    items.remove(id);
                } else {
                    existingItem.setQuantity(newQuantity);
                }
            }
        }
    }

    public void removeItem(Long cartItemId) {
        synchronized (lock) {
            items.remove(cartItemId);
        }
    }

    public void clearCart(){
        synchronized (lock) {
            items.clear();
        }
    }

    public BigDecimal getSubTotal(){
        synchronized (lock) {
            BigDecimal subTotal = BigDecimal.ZERO;

            for (CartItem item : items.values()) {
                BigDecimal itemPrice = item.getProduct().getPrice();
                BigDecimal itemQty = new BigDecimal(item.getQuantity());
                subTotal = subTotal.add(itemPrice.multiply(itemQty));
            }
            return subTotal;
        }
    }

    public int getTotalItemsCount(){
        synchronized (lock) {
            return items.values().stream().mapToInt(CartItem::getQuantity).sum();
        }
    }

    public String getCartSnapshotSignature(){
        synchronized (lock) {
            StringBuilder sb = new StringBuilder();

            for (CartItem item : items.values()) {
                sb.append(item.getProduct().getId());
                sb.append(":");
                sb.append(item.getQuantity());
                sb.append(",");
            }
            return sb.toString();
        }
    }
}