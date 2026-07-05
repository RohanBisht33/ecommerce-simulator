package com.ecommerce.controller;

import com.ecommerce.service.CartService;
import com.ecommerce.util.OrderFinancials;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String showCartFront(Model model){
        model.addAttribute("cartItems", cartService.getCartItems());

        BigDecimal subtotal = cartService.getSubTotal();
        BigDecimal shippingCost = OrderFinancials.calculateShipping(subtotal);
        BigDecimal tax = OrderFinancials.calculateTax(subtotal);
        BigDecimal total = OrderFinancials.calculateTotal(subtotal);

        model.addAttribute("totalItems", cartService.getTotalItemsCount());
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("discount", BigDecimal.ZERO);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/update/{cartItemId}")
    public String updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestParam String action,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            if ("increase".equals(action)) {
                cartService.increaseQuantity(cartItemId);
            } else if ("decrease".equals(action)) {
                cartService.decreaseQuantity(cartItemId);
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{cartItemId}")
    public String removeCartItem(@PathVariable Long cartItemId) {
        cartService.removeItem(cartItemId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart() {
        cartService.clearCart();
        return "redirect:/cart";
    }

    @PostMapping("/cart/promo")
    public String applyPromoCode(@RequestParam String promoCode,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        // Promo codes are not implemented yet; show a message to the user
        redirectAttributes.addFlashAttribute("promoError", "Promo codes are not available yet. Stay tuned!");
        redirectAttributes.addFlashAttribute("appliedPromo", promoCode);
        return "redirect:/cart";
    }
}