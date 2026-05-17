package com.ecommerce.order.DTOs;

import com.ecommerce.enums.OrderStatus;

import com.ecommerce.user.model.User;
import com.ecommerce.orderItem.model.OrderItem;
import lombok.Data;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Size;

@Data
public class OrderUpdateDTO {

    @Size(max = 30)
    private User customer;
    
    private List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

}