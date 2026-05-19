package com.ecommerce.projetobackend.product;

import com.ecommerce.model.Product;
import com.ecommerce.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Busca todos os produtos de um vendedor específico
    List<Product> findBySellerId(Long sellerId);

    // Busca produtos por status (ex: só os ACTIVE)
    List<Product> findByStatus(ProductStatus status);

    // Busca produtos de um vendedor com um status específico
    List<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status);
}