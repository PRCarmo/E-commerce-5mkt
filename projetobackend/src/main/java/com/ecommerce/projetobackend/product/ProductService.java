package com.ecommerce.projetobackend.product;

import com.ecommerce.model.Product;
import com.ecommerce.model.ProductStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    // O Spring injeta o repository automaticamente aqui
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Retorna todos os produtos
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // Busca um produto pelo ID, lança exceção se não encontrar
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com id: " + id));
    }

    // Cria um novo produto
    public Product create(Product product) {
        product.setStatus(ProductStatus.ACTIVE); // todo produto começa ACTIVE
        return productRepository.save(product);
    }

    // Atualiza um produto existente
    public Product update(Long id, Product updatedProduct) {
        Product existing = findById(id); // já lança exceção se não existir

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStock(updatedProduct.getStock());
        existing.setStatus(updatedProduct.getStatus());

        return productRepository.save(existing);
    }

    // Deleta um produto pelo ID
    public void delete(Long id) {
        findById(id); // garante que existe antes de deletar
        productRepository.deleteById(id);
    }

    // Retorna todos os produtos de um vendedor
    public List<Product> findBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }
}