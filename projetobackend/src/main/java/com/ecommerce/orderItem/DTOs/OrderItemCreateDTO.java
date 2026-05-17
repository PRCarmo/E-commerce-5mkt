package com.ecommerce.orderItem.DTOs;

import java.math.BigDecimal;
import com.ecommerce.product.model.Product;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemCreateDTO {
    
    @NotNull
    private Product product;

    @NotNull
    private Integer quantity;

    @NotNull
    private BigDecimal unitPrice;

}
