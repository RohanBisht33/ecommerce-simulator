package com.ecommerce.service;

import com.ecommerce.entity.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED, OrderStatus.REFUNDED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.REFUNDED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class)
    );

    public boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        Set<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    public boolean requiresStockDeduction(OrderStatus from, OrderStatus to) {
        return from == OrderStatus.PENDING && to == OrderStatus.PROCESSING;
    }

    public boolean requiresStockRestoration(OrderStatus from, OrderStatus to) {
        if (to != OrderStatus.CANCELLED && to != OrderStatus.REFUNDED) {
            return false;
        }
        return from == OrderStatus.PROCESSING
                || from == OrderStatus.SHIPPED
                || from == OrderStatus.DELIVERED;
    }
}
