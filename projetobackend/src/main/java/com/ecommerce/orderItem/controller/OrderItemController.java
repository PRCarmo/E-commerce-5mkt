package com.ecommerce.orderItem.controller;

import com.ecommerce.orderItem.DTOs.*;
import com.ecommerce.orderItem.service.OrderItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/orderItem")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponseDTO post(
        
        @RequestBody OrderItemCreateDTO dto
    ) {
        
        return service.post(dto);
    }

    @GetMapping
    public List<OrderItemResponseDTO> get() {
        
        return service.get();
    }

    @GetMapping("/{id}")
    public OrderItemResponseDTO getById(

        @PathVariable Long id
    ) {

        return service.getById(id);
    }

    @PutMapping("/{id}")
    public OrderItemResponseDTO put(
        
        @PathVariable Long id,
        @RequestBody OrderItemUpdateDTO dto
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
