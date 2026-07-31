package com.ecommerce.service;

import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, UserRepository userRepository,
                       ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    private User getCurrentUser() {
        User user = getCurrentUserOrNull();
        if (user == null) {
            throw new IllegalStateException("A logged-in user is required for cart operations");
        }
        return user;
    }

    private User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsernameOrEmail(auth.getName(), auth.getName()).orElse(null);
    }

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

    public int getQuantityInCart(Long productId) {
        User user = getCurrentUserOrNull();
        if (user == null) {
            return 0;
        }
        return cartItemRepository.findByUserAndProductId(user, productId)
                .map(CartItem::getQuantity)
                .orElse(0);
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
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        checkOwnership(item, user);

        Product product = productRepository.findProductForUpdate(item.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStock() < item.getQuantity() + 1) {
            throw new IllegalArgumentException(
                    "Insufficient inventory available for product: " + product.getName());
        }

        item.setQuantity(item.getQuantity() + 1);
        cartItemRepository.save(item);
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
