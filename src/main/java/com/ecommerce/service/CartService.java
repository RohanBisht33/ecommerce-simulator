package com.ecommerce.service;

import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

/**
 * Cart is now persisted in the database, keyed to the logged-in user,
 * instead of living in an in-memory, session-scoped map. This is what
 * makes the cart survive logout/login and separate browser sessions.
 */
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository, UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    // Used by methods that MUST have a real user (adding/changing/removing items).
    private User getCurrentUser() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new IllegalStateException("A logged-in user is required for cart operations");
        }
        return user;
    }

    // Used by read-only methods that also run on public pages (e.g. the storefront
    // cart badge), which anonymous visitors can see. Returns null instead of
    // throwing so callers can treat a guest as having an empty cart.
    private User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    // Defense in depth: since cart items are now rows in a shared table keyed
    // by numeric id, a request could try to increase/decrease/remove another
    // user's cart item by guessing its id. Verify ownership before any mutation.
    private void checkOwnership(CartItem item, User currentUser) {
        if (!item.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Cart item does not belong to the current user");
        }
    }

    public Collection<CartItem> getCartItems() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            return Collections.emptyList();
        }
        return cartItemRepository.findByUser(user);
    }

    @Transactional
    public void addProduct(Product product) {
        User user = getCurrentUser();
        CartItem existingItem = cartItemRepository.findByUserAndProductId(user, product.getId()).orElse(null);

        if (existingItem == null) {
            cartItemRepository.save(new CartItem(user, product, 1));
        } else {
            existingItem.setQuantity(existingItem.getQuantity() + 1);
            cartItemRepository.save(existingItem);
        }
    }

    @Transactional
    public void increaseQuantity(Long cartItemId) {
        User user = getCurrentUser();
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            checkOwnership(item, user);
            item.setQuantity(item.getQuantity() + 1);
            cartItemRepository.save(item);
        });
    }

    @Transactional
    public void decreaseQuantity(Long cartItemId) {
        User user = getCurrentUser();
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            checkOwnership(item, user);
            int newQuantity = item.getQuantity() - 1;
            if (newQuantity <= 0) {
                cartItemRepository.delete(item);
            } else {
                item.setQuantity(newQuantity);
                cartItemRepository.save(item);
            }
        });
    }

    @Transactional
    public void removeItem(Long cartItemId) {
        User user = getCurrentUser();
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            checkOwnership(item, user);
            cartItemRepository.delete(item);
        });
    }

    @Transactional
    public void clearCart() {
        cartItemRepository.deleteByUser(getCurrentUser());
    }

    public BigDecimal getSubTotal() {
        BigDecimal subTotal = BigDecimal.ZERO;

        for (CartItem item : getCartItems()) {
            BigDecimal itemPrice = item.getProduct().getPrice();
            BigDecimal itemQty = new BigDecimal(item.getQuantity());
            subTotal = subTotal.add(itemPrice.multiply(itemQty));
        }
        return subTotal;
    }

    public int getTotalItemsCount() {
        return getCartItems().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public String getCartSnapshotSignature() {
        StringBuilder sb = new StringBuilder();

        for (CartItem item : getCartItems()) {
            sb.append(item.getProduct().getId());
            sb.append(":");
            sb.append(item.getQuantity());
            sb.append(",");
        }
        return sb.toString();
    }
}