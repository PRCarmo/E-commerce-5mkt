package com.ecommerce.projetobackend.product;

import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> list(Long sellerId, ProductStatus status, Pageable pageable) {
        if (sellerId != null && status != null) {
            return productRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        }
        if (sellerId != null) {
            return productRepository.findBySellerId(sellerId, pageable);
        }
        if (status != null) {
            return productRepository.findByStatus(status, pageable);
        }
        return productRepository.findAll(pageable);
    }

    public Product findById(Long id) {
        return productRepository.findByIdWithSeller(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
    }

    public Product create(ProductCreateRequest request, User authenticatedUser) {
        if (authenticatedUser.getRole() != UserRole.SELLER) {
            throw new ForbiddenException("Only sellers can create products");
        }
        Product product = Product.builder()
                .seller(authenticatedUser)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .status(ProductStatus.ACTIVE)
                .build();
        Product saved = productRepository.save(product);
        return productRepository.findByIdWithSeller(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("Product", saved.getId()));
    }

    public Product update(Long id, ProductUpdateRequest request, User authenticatedUser) {
        Product product = findById(id);
        checkOwnership(product, authenticatedUser);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(request.getStatus());
        productRepository.save(product);
        return productRepository.findByIdWithSeller(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
    }

    public void delete(Long id, User authenticatedUser) {
        Product product = findById(id);
        checkOwnership(product, authenticatedUser);
        productRepository.delete(product);
    }

    private void checkOwnership(Product product, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!product.getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to modify this product");
        }
    }
}
