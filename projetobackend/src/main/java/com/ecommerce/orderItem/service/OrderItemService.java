package com.ecommerce.orderItem.service;

import com.ecommerce.orderItem.DTOs.*;
import com.ecommerce.orderItem.model.OrderItem;
import com.ecommerce.orderItem.mapper.OrderItemMapper;
import com.ecommerce.orderItem.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
// import RuntimeException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository repository;
    private final OrderItemMapper mapper;

    public OrderItemResponseDTO post(OrderItemCreateDTO dto) {
        OrderItem orderItem = mapper.toEntity(dto);

        OrderItem saved = repository.save(orderItem);

        return mapper.toResponseDto(saved);
    }

    public List<OrderItemResponseDTO> get() {
        
        return repository.findAll()
            .stream()
            .map(mapper::toResponseDto)
            .toList();
    }

    public OrderItemResponseDTO getById(
        Long id
    ) {
        OrderItem orderItem = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Order item not found")
        );

        return mapper.toResponseDto(orderItem);
    }

    public OrderItemResponseDTO put(
        Long id, OrderItemUpdateDTO dto
    ) {
        
        OrderItem orderItem = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Order item not found")
        );

        mapper.updateEntityFromDto(dto, orderItem);
        OrderItem updated = repository.save(orderItem);
        
        return mapper.toResponseDto(updated);
    }

    public void delete(
        Long id
    ) {
        OrderItem orderItem = repository.findById(id)
            .orElseThrow(() ->
            new RuntimeException("Order item not found")
        );

        repository.delete(orderItem);
    }
}