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

    private Map<Long, CartItem> items = new HashMap<>();

    public Collection<CartItem> getCartItems(){
        return items.values();
    }

    public void addProduct(Product product){
        CartItem existingItem = items.get(product.getId());

        if(existingItem == null){
            items.put(product.getId(), new CartItem(product.getId(), product, 1));
        }
        else{
            existingItem.setQuantity(existingItem.getQuantity() + 1);
        }
    }
    public void increaseQuantity(Long id) {
        CartItem existingItem = items.get(id);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + 1);
        }
    }

    public void decreaseQuantity(Long id) {
        CartItem existingItem = items.get(id);

        if (existingItem != null) {
            if (existingItem.getQuantity() <= 0) {
                items.remove(id);
            } else {
                existingItem.setQuantity(existingItem.getQuantity() - 1);
            }
        }
    }
    public void clearCart(){
        items.clear();
    }
    public BigDecimal getSubTotal(){
        BigDecimal subTotal = BigDecimal.ZERO;

        for(CartItem item : items.values()){
            BigDecimal itemPrice = item.getProduct().getPrice();
            BigDecimal itemQty = new BigDecimal(item.getQuantity());
            subTotal = subTotal.add(itemPrice.multiply(itemQty));
        }
        return subTotal;
    }

    public int getTotalItemsCount(){
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }
}
