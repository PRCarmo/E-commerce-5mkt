package com.ecommerce.orderItem.DTOs;

import java.math.BigDecimal;
import com.ecommerce.product.model.Product;
import lombok.Data;

@Data
public class OrderItemResponseDTO {

    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;
    
}