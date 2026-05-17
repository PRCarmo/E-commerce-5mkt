package com.ecommerce.order.DTOs;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.orderItem.model.OrderItem;
import com.ecommerce.user.model.User;
import lombok.Data;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponseDTO {

    private User customer;
    private List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    
}