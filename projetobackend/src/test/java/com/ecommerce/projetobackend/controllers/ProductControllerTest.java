package com.ecommerce.projetobackend.controllers;

import com.ecommerce.projetobackend.auth.LoginRequest;
import com.ecommerce.projetobackend.config.SecurityConfig;
import com.ecommerce.projetobackend.product.*;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import com.ecommerce.projetobackend.auth.AuthResponse;
import com.ecommerce.projetobackend.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private AuthService authService;

    @Test
    void mustGetProductByIdSuccessfully() throws Exception {

        User seller = new User();
        seller.setId(10L);
        seller.setName("Seller Test");

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook");
        product.setDescription("Gaming Notebook");
        product.setPrice(BigDecimal.valueOf(5000));
        product.setStock(15);
        product.setStatus(ProductStatus.ACTIVE);
        product.setSeller(seller);

        when(productService.findById(1L))
                .thenReturn(product);

        mockMvc.perform(
                get("/api/v1/products/1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Notebook"))
        .andExpect(jsonPath("$.description").value("Gaming Notebook"))
        .andExpect(jsonPath("$.price").value(5000))
        .andExpect(jsonPath("$.stock").value(15))
        .andExpect(jsonPath("$.sellerId").value(10))
        .andExpect(jsonPath("$.sellerName").value("Seller Test"));
    }

    @Test
    void mustListProductsSuccessfully() throws Exception {

        User seller = new User();
        seller.setId(10L);
        seller.setName("Test Seller");

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(BigDecimal.valueOf(5000));
        product.setStock(15);
        product.setStatus(ProductStatus.ACTIVE);
        product.setSeller(seller);

        Page<Product> page =
                new PageImpl<>(List.of(product));

        when(productService.list(
                eq(10L),
                eq(ProductStatus.ACTIVE),
                any(Pageable.class)
        ))
        .thenReturn(page);

        mockMvc.perform(
                get("/api/v1/products")
                        .param("sellerId", "10")
                        .param("status", "ACTIVE")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id")
                .value(1))
        .andExpect(jsonPath("$.content[0].name")
                .value("Test Product"))
        .andExpect(jsonPath("$.content[0].sellerId")
                .value(10))
        .andExpect(jsonPath("$.content[0].sellerName")
                .value("Test Seller"));

        
    }

    @Test
    void mustCreateProductSuccessfully() throws Exception {

        User seller = new User();
        seller.setId(10L);
        seller.setName("Test Seller");
        seller.setRole(UserRole.SELLER);

        UserDetailsImpl userDetails = new UserDetailsImpl(seller);

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Test Product");
        request.setDescription("Product Description");
        request.setPrice(BigDecimal.valueOf(5000));
        request.setStock(15);

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(BigDecimal.valueOf(5000));
        product.setStock(15);
        product.setStatus(ProductStatus.ACTIVE);
        product.setSeller(seller);

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        when(productService.create(
                any(ProductCreateRequest.class),
                any(User.class)
        )).thenReturn(product);

        mockMvc.perform(
                post("/api/v1/products")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Test Product"))
        .andExpect(jsonPath("$.sellerId").value(10));

        verify(productService).create(
                any(ProductCreateRequest.class),
                any(User.class)
        );
    }

    @Test
    void mustUpdateProductSuccessfully() throws Exception {

        User seller = new User();
        seller.setId(10L);
        seller.setName("Seller Test");
        seller.setRole(UserRole.SELLER);

        UserDetailsImpl userDetails = new UserDetailsImpl(seller);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Notebook Updated");
        request.setDescription("Updated");
        request.setPrice(BigDecimal.valueOf(7000));
        request.setStock(20);
        request.setStatus(ProductStatus.ACTIVE);

        Product product = new Product();
        product.setId(1L);
        product.setName("Notebook Updated");
        product.setDescription("Updated");
        product.setPrice(BigDecimal.valueOf(7000));
        product.setStock(20);
        product.setStatus(ProductStatus.ACTIVE);
        product.setSeller(seller);

        when(productService.update(
                eq(1L),
                any(ProductUpdateRequest.class),
                any(User.class)
        )).thenReturn(product);

        mockMvc.perform(
                put("/api/v1/products/1")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name")
                .value("Notebook Updated"));

        verify(productService).update(
                eq(1L),
                any(ProductUpdateRequest.class),
                any(User.class)
        );
    }

    @Test
    void mustDeleteProductSuccessfully() throws Exception {

        User seller = new User();
        seller.setId(10L);
        seller.setRole(UserRole.SELLER);

        UserDetailsImpl userDetails = new UserDetailsImpl(seller);

        mockMvc.perform(
                delete("/api/v1/products/1")
                        .with(user(userDetails))
        )
        .andExpect(status().isNoContent());

        verify(productService)
                .delete(eq(1L), any(User.class));
    }

    @Test
    void mustRejectCreateWhenNameIsBlank() throws Exception {

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("");
        request.setDescription("Description");
        request.setPrice(BigDecimal.valueOf(5000));
        request.setStock(10);

        mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void mustRejectCreateWhenPriceIsInvalid() throws Exception {

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Notebook");
        request.setDescription("Description");
        request.setPrice(BigDecimal.ZERO);
        request.setStock(10);

        mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void mustRejectCreateWhenStockIsNegative() throws Exception {

        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Notebook");
        request.setDescription("Description");
        request.setPrice(BigDecimal.valueOf(5000));
        request.setStock(-1);

        mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void mustReturn404WhenProductDoesNotExist() throws Exception {

        when(productService.findById(999L))
                .thenThrow(
                        new EntityNotFoundException(
                                "Product",
                                999L
                        )
                );

        mockMvc.perform(
                get("/api/v1/products/999")
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void mustReturn403WhenUserIsNotOwner() throws Exception {

        User seller = new User();
        seller.setId(1L);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(seller);

        ProductUpdateRequest request =
                new ProductUpdateRequest();

        when(productService.update(
                eq(1L),
                any(ProductUpdateRequest.class),
                any(User.class)
        ))
        .thenThrow(
                new ForbiddenException(
                        "You do not have permission to modify this product"
                )
        );

        mockMvc.perform(
                put("/api/v1/products/1")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mustReturn403WhenCustomerTriesToCreateProduct() throws Exception {

        User customer = new User();
        customer.setId(1L);
        customer.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        ProductCreateRequest request =
                new ProductCreateRequest();

        request.setName("Notebook");
        request.setDescription("Description");
        request.setPrice(BigDecimal.valueOf(5000));
        request.setStock(10);

        when(productService.create(
                any(ProductCreateRequest.class),
                any(User.class)
        ))
        .thenThrow(
                new ForbiddenException(
                        "Only sellers can create products"
                )
        );

        mockMvc.perform(
                post("/api/v1/products")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isForbidden());
    }
}
