package com.ecommerce.orderItem.mapper;

import com.ecommerce.orderItem.model.OrderItem;
import com.ecommerce.orderItem.DTOs.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrderItemMapper {
    OrderItem toEntity(OrderItemCreateDTO createDTO);

    OrderItemResponseDTO toResponseDto(OrderItem entity);

    void updateEntityFromDto(OrderItemUpdateDTO updateDto, @MappingTarget OrderItem entity);
}