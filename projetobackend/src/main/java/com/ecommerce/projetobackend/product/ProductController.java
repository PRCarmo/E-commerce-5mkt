package com.ecommerce.projetobackend.product;

import com.ecommerce.model.Product;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /products → lista todos os produtos
    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    // GET /products/{id} → busca um produto pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    // POST /products → cria um novo produto
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product created = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /products/{id} → atualiza um produto existente
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    // DELETE /products/{id} → deleta um produto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /products/seller/{sellerId} → produtos de um vendedor específico
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Product>> findBySeller(@PathVariable Long sellerId) {
        return ResponseEntity.ok(productService.findBySeller(sellerId));
    }
}