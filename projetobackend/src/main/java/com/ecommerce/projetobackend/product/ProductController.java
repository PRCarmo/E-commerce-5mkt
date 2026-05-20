package com.ecommerce.projetobackend.product;

import com.ecommerce.projetobackend.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> list(
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) ProductStatus status,
            Pageable pageable) {
        Page<ProductResponse> page = productService.list(sellerId, status, pageable)
                .map(ProductResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ProductResponse.from(productService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Product product = productService.create(request, userDetails.getUser());
        return ResponseEntity
                .created(URI.create("/api/v1/products/" + product.getId()))
                .body(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Product product = productService.update(id, request, userDetails.getUser());
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        productService.delete(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}
