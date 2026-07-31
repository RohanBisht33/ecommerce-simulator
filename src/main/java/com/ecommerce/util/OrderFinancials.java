package com.ecommerce.util;

import java.math.BigDecimal;

public final class OrderFinancials {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("5.99");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05");

    private OrderFinancials() {
    }

    public static BigDecimal calculateShipping(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING;
    }

    public static BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal) {
        return subtotal.add(calculateShipping(subtotal)).add(calculateTax(subtotal));
    }
}
