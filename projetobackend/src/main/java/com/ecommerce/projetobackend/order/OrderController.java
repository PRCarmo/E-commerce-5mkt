package com.ecommerce.projetobackend.order;

import com.ecommerce.projetobackend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Order order = orderService.create(request, userDetails.getUser());
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> list(
            Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Page<OrderResponse> page = orderService.list(userDetails.getUser(), pageable)
                .map(OrderResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Order order = orderService.findById(id, userDetails.getUser());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Order order = orderService.cancel(id, userDetails.getUser());
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
