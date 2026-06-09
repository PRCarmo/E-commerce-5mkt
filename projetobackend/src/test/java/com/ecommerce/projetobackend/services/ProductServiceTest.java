package com.ecommerce.projetobackend.services;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ecommerce.projetobackend.product.*;
import com.ecommerce.projetobackend.auth.AuthService;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.user.UserRole;
import com.ecommerce.projetobackend.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    
    @Mock
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCreateRequest productCreateRequest;

    @InjectMocks
    private AuthService authService;

    @Test
    void mustListAll() {

        Pageable pageable = PageRequest.of(0, 10);

        productService.list(null, null, pageable);

        verify(productRepository)
                .findAll(pageable);
    }

    @Test
    void mustGetProductById() {

        Product product = new Product();

        when(productRepository.findByIdWithSeller(1L))
                .thenReturn(Optional.of(product));

        Product result = productService.findById(1L);

        assertEquals(product, result);
    }

    @Test
    void mustThrowExceptionWhenProductDoesNotExist() {

        when(productRepository.findByIdWithSeller(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productService.findById(1L)
        );
    }

    @Test
    void mustForbidCostumerOfCreatingProduct() {
        
        User user = new User();
        user.setName("Teste");
        user.setEmail("teste@email.com");
        user.setPassword("12345678");
        user.setRole(UserRole.CUSTOMER);

        ProductCreateRequest request = new ProductCreateRequest();

        assertThrows(
                ForbiddenException.class,
                () -> productService.create(request, user)
        );
    }

    @Test
    void mustForbidCostumerOfUpdatingProduct() {
        User user = new User();
        user.setName("Teste");
        user.setEmail("teste@email.com");
        user.setPassword("12345678");
        user.setRole(UserRole.CUSTOMER);

        ProductUpdateRequest request = new ProductUpdateRequest();

        assertThrows(ForbiddenException.class, 
            () -> productService.update(null, request, user)
        );
        
    }

    @Test
    void mustForbidCostumerOfDeletingProduct() {
        User user = new User();
        user.setName("Teste");
        user.setEmail("teste@email.com");
        user.setPassword("12345678");
        user.setRole(UserRole.CUSTOMER);

        assertThrows(ForbiddenException.class,
            () -> productService.delete(null, user)
        );
    }

    @Test
    void mustCreateProductSuccessfully() {

        User seller = new User();
        seller.setId(1L);
        seller.setRole(UserRole.SELLER);

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Test");
        request.setDescription("TestDescription");
        request.setPrice(BigDecimal.valueOf(1));
        request.setStock(1);

        Product savedProduct = Product.builder()
                .id(10L)
                .seller(seller)
                .name("Test")
                .description("TestDescription")
                .price(BigDecimal.valueOf(1))
                .stock(1)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        when(productRepository.findByIdWithSeller(10L))
                .thenReturn(Optional.of(savedProduct));

        Product result =
                productService.create(request, seller);

        assertEquals("Test", result.getName());

        verify(productRepository)
                .save(any(Product.class));
    }


    @Test
    void mustAllowSellerToUpdateProduct() {
        User seller = new User();
        seller.setId(1L);
        seller.setName("Test");
        seller.setEmail("test@email.com");
        seller.setPassword("12345678");
        seller.setRole(UserRole.SELLER);

        Product product = new Product();
        product.setId(10L);
        product.setSeller(seller);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("TestProduct");
        request.setDescription("TestDescription");
        request.setPrice(BigDecimal.valueOf(7000));
        request.setStock(12);
        request.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findByIdWithSeller(10L))
                .thenReturn(Optional.of(product))
                .thenReturn(Optional.of(product));

        productService.update(10L, request, seller);

        ArgumentCaptor<Product> captor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository)
                .save(captor.capture());

        Product salvo = captor.getValue();

        assertEquals("TestProduct", salvo.getName());
        assertEquals("TestDescription", salvo.getDescription());
        assertEquals(
                BigDecimal.valueOf(7000),
                salvo.getPrice()
        ); 
        assertEquals(12, salvo.getStock());
        assertEquals(ProductStatus.ACTIVE, salvo.getStatus());
    }

    @Test
    void mustForbidDeleteForNonOwnerSeller() {

        User sellerLogado = new User();
        sellerLogado.setId(1L);
        sellerLogado.setRole(UserRole.SELLER);

        User sellerDono = new User();
        sellerDono.setId(2L);
        sellerDono.setRole(UserRole.SELLER);

        Product product = new Product();
        product.setId(10L);
        product.setSeller(sellerDono);

        when(productRepository.findByIdWithSeller(10L))
                .thenReturn(Optional.of(product));

        ForbiddenException exception =
                assertThrows(
                        ForbiddenException.class,
                        () -> productService.delete(10L, sellerLogado)
                );

        assertEquals(
                "You do not have permission to modify this product",
                exception.getMessage()
        );

        verify(productRepository, never())
                .delete(any(Product.class));
    }
 
    @Test
    void mustAllowAdminToDeleteProduct() {
        
        User admin = new User();
        admin.setId(1L);
        admin.setName("Test");
        admin.setEmail("test@email.com");
        admin.setPassword("12345678");
        admin.setRole(UserRole.ADMIN);

        User seller = new User();
        seller.setId(2L);

        Product product = new Product();
        product.setId(3L);

        when(productRepository.findByIdWithSeller(2L));

        productService.delete(3L, admin);

        verify(productRepository)
                .delete(product);
    }
}