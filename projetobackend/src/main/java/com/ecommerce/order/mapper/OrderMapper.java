package com.ecommerce.order.mapper;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.DTOs.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrderMapper {
    Order toEntity(OrderCreateDTO createDTO);

    OrderResponseDTO toResponseDto(Order entity);

    void updateEntityFromDto(OrderUpdateDTO updateDto, @MappingTarget Order entity);
}