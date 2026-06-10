package com.ecommerce.projetobackend.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse from(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getPrice();
        return CartItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
