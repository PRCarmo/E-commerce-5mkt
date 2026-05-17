package com.ecommerce.product.service;

import com.ecommerce.product.DTOs.*;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
// import RuntimeException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductResponseDTO post(ProductCreateDTO dto) {
        Product product = mapper.toEntity(dto);

        Product saved = repository.save(product);

        return mapper.toResponseDto(saved);
    }

    public List<ProductResponseDTO> get() {
        
        return repository.findAll()
            .stream()
            .map(mapper::toResponseDto)
            .toList();
    }

    public ProductResponseDTO getById(
        Long id
    ) {
        Product product = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Product not found")
        );

        return mapper.toResponseDto(product);
    }

    public ProductResponseDTO put(
        Long id, ProductUpdateDTO dto
    ) {
        
        Product product = repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Product not found")
        );

        mapper.updateEntityFromDto(dto, product);
        Product updated = repository.save(product);
        
        return mapper.toResponseDto(updated);
    }

    public void delete(
        Long id
    ) {
        Product product = repository.findById(id)
            .orElseThrow(() ->
            new RuntimeException("Product item not found")
        );

        repository.delete(product);
    }
}