package com.ecommerce.projetobackend.cart;

import com.ecommerce.projetobackend.order.Order;
import com.ecommerce.projetobackend.order.OrderResponse;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Cart cart = cartService.getCart(userDetails.getUser());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Cart cart = cartService.addItem(userDetails.getUser(), request);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Cart cart = cartService.updateItem(userDetails.getUser(), productId, request);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        cartService.removeItem(userDetails.getUser(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        cartService.clear(userDetails.getUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Order order = cartService.checkout(userDetails.getUser());
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }
}
