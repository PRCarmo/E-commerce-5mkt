package com.ecommerce.order.DTOs;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.orderItem.model.OrderItem;
import com.ecommerce.user.model.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class OrderCreateDTO {

    @NotNull
    @Size(max = 30)
    private User customer;

    @NotEmpty
    private List<OrderItem> items;

    @NotNull
    private OrderStatus status;

    @NotNull
    private BigDecimal totalAmount;
    
    @NotNull
    private LocalDateTime createdAt;
}