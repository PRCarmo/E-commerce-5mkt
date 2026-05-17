package com.ecommerce.product.controller;

import com.ecommerce.product.DTOs.*;
import com.ecommerce.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO post(
        
        @RequestBody ProductCreateDTO dto
    ) {
        
        return service.post(dto);
    }

    @GetMapping
    public List<ProductResponseDTO> get() {
        
        return service.get();
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getById(
        
        @PathVariable Long id
    ) {

        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ProductResponseDTO put(

        @PathVariable Long id,
        @RequestBody ProductUpdateDTO dto
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
