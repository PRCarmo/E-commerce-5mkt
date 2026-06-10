package com.ecommerce.projetobackend.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long customerId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartResponse.builder()
                .id(cart.getId())
                .customerId(cart.getCustomer().getId())
                .items(items)
                .totalAmount(totalAmount)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}
