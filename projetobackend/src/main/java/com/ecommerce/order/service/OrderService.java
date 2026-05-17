package com.ecommerce.order.service;

import com.ecommerce.order.DTOs.*;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
// import RuntimeException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;

    public OrderResponseDTO post(OrderCreateDTO dto) {
        Order order = mapper.toEntity(dto);

        Order saved = repository.save(order);

        return mapper.toResponseDto(saved);
    }

    public List<OrderResponseDTO> get() {
        
        return repository.findAll()
            .stream()
            .map(mapper::toResponseDto)
            .toList();
    }

    public OrderResponseDTO getById(
        Long id
    ) {
        Order order = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Order not found")
        );

        return mapper.toResponseDto(order);
    }

    public OrderResponseDTO put(
        Long id, OrderUpdateDTO dto
    ) {
        
        Order order = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Order not found")
        );

        mapper.updateEntityFromDto(dto, order);
        Order updated = repository.save(order);
        
        return mapper.toResponseDto(updated);
    }

    public void delete(
        Long id
    ) {
        Order order = repository.findById(id)
            .orElseThrow(() ->
            new RuntimeException("Order not found")
        );

        repository.delete(order);
    }
}