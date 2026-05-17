package com.ecommerce.order.controller;

import com.ecommerce.order.DTOs.*;
import com.ecommerce.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;


import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDTO post(
        
        @RequestBody OrderCreateDTO dto
    ) {
        
        return service.post(dto);
    }

    @GetMapping
    public List<OrderResponseDTO> get() {
        
        return service.get();
    }

    @GetMapping("/{id}")
    public OrderResponseDTO getById(
        @PathVariable Long id
    ) {

        return service.getById(id);
    }

    @PutMapping("/{id}")
    public OrderResponseDTO put(
        @PathVariable Long id,
        @RequestBody OrderUpdateDTO dto
    ) {

        return service.put(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id
    ) {

        service.delete(id);
    }
}
