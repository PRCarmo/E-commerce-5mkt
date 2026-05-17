package com.ecommerce.product.mapper;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.DTOs.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {
    Product toEntity(ProductCreateDTO createDTO);

    ProductResponseDTO toResponseDto(Product entity);

    void updateEntityFromDto(ProductUpdateDTO updateDto, @MappingTarget Product entity);
}