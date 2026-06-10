package com.ecommerce.projetobackend.services;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import com.ecommerce.projetobackend.order.*;
import com.ecommerce.projetobackend.product.Product;
import com.ecommerce.projetobackend.product.ProductRepository;
import com.ecommerce.projetobackend.product.ProductStatus;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.shared.exception.InsufficientStockException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderStateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // EntityManager é injetado via @PersistenceContext (campo), que o
        // @InjectMocks não preenche quando usa injeção por construtor.
        ReflectionTestUtils.setField(orderService, "entityManager", entityManager);
    }

    @Test
    void mustDenyOrderCreationForNonCustomer() {
        User seller = new User();
        seller.setId(1L);
        seller.setRole(UserRole.SELLER);

        OrderCreateRequest request = new OrderCreateRequest(); 
        
        ForbiddenException exception =
        assertThrows(ForbiddenException.class, 
            () -> orderService.create(request, seller));

        assertEquals(
            "Only customers can create orders",
            exception.getMessage()
        );

        verifyNoInteractions(productRepository);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void mustRejectOrderWhenStockIsInsufficient() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(1);
        product.setPrice(BigDecimal.TEN);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setItems(List.of(itemRequest));

        when(productRepository.findByIdWithSeller(1L))
                .thenReturn(Optional.of(product));

        InsufficientStockException exception =
        assertThrows(
            InsufficientStockException.class,
            () -> orderService.create(request, user)
        );

        assertEquals(1, product.getStock());

        assertEquals(
            "Product 1 has insufficient stock",
            exception.getMessage()
        );

        verify(productRepository)
            .findByIdWithSeller(1L);

        verify(productRepository, never())
                .save(any(Product.class));
    }
    
    @Test
    void mustRejectInactiveProduct() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        Product product = new Product();
        product.setId(2L);
        product.setStatus(ProductStatus.INACTIVE);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(2L);
        itemRequest.setQuantity(1);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setItems(List.of(itemRequest));

        when(productRepository.findByIdWithSeller(2L))
                .thenReturn(Optional.of(product));

        InvalidOrderException exception =
        assertThrows(
            InvalidOrderException.class,
            () -> orderService.create(request, user)
        );

        assertEquals("Product 2 is not active",
            exception.getMessage()
        );
    }
    
    @Test
    void mustAllowCustomerToCreateOrder() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(10);
        product.setPrice(BigDecimal.valueOf(100));
       
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);
        
        OrderCreateRequest request = new OrderCreateRequest();
        request.setItems(List.of(itemRequest));

        Order savedOrder = Order.builder()
            .id(99L)
            .customer(user)
            .status(OrderStatus.PENDING)
            .totalAmount(BigDecimal.valueOf(200))
            .build();

        when(productRepository.findByIdWithSeller(1L))
            .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
            .thenReturn(savedOrder);

        when(orderRepository.findByIdWithDetails(99L))
                .thenReturn(Optional.of(savedOrder));

        Order result = orderService.create(request, user);

        assertEquals(
            BigDecimal.valueOf(200), 
            result.getTotalAmount()
        );

        assertEquals(
            OrderStatus.PENDING,
            result.getStatus()
        );
    }
    
    @Test
    void mustCalculateOrderTotalCorrectly() {

        User customer = new User();
        customer.setId(1L);
        customer.setRole(UserRole.CUSTOMER);

        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(10);
        product.setPrice(BigDecimal.valueOf(100));

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setItems(List.of(itemRequest));

        Order savedOrder = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(300))
                .build();

        when(productRepository.findByIdWithSeller(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        when(orderRepository.findByIdWithDetails(1L))
                .thenReturn(Optional.of(savedOrder));

        Order result =
                orderService.create(request, customer);

        assertEquals(
                BigDecimal.valueOf(300),
                result.getTotalAmount()
        );

        assertEquals(
                7,
                product.getStock()
        );

        verify(productRepository)
                .save(product);
    }
    @Test
    void mustAllowCustomerToCancelOwnPendingOrder() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        Order order = new Order();
        order.setId(2L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdWithDetails(2L))
            .thenReturn(Optional.of(order))
            .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
            .thenReturn(order);

        Order result = orderService.cancel(2L, user);

        assertEquals(
            OrderStatus.CANCELLED, 
            result.getStatus()
        );

        verify(orderRepository).save(order);
 
    }
    
    @Test
    void mustRejectCancellationForNonPendingOrder() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        Order order = new Order();
        order.setId(2L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findByIdWithDetails(2L))
            .thenReturn(Optional.of(order));

        assertThrows(
            InvalidOrderStateException.class,
            () -> orderService.cancel(2L, user)
        );
    }
     
    @Test
    void mustRestoreStockWhenOrderIsCancelled() {

        User customer = new User();
        customer.setId(1L);
        customer.setRole(UserRole.CUSTOMER);

        Product product = new Product();
        product.setId(1L);
        product.setStock(8);

        Order order = new Order();
        order.setId(2L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);

        order.getItems().add(item);

        when(orderRepository.findByIdWithDetails(2L))
                .thenReturn(Optional.of(order))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        orderService.cancel(2L, customer);

        assertEquals(
                10,
                product.getStock()
        );

        assertEquals(
                OrderStatus.CANCELLED,
                order.getStatus()
        );

        verify(productRepository)
                .save(product);

        verify(orderRepository)
                .save(order);
    }
    
    @Test
    void mustDenyCancelForOtherCustomer() {
        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setRole(UserRole.CUSTOMER);

        User orderOwner = new User();
        orderOwner.setId(2L);

        Order order = new Order();
        order.setId(3L);
        order.setCustomer(orderOwner);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdWithDetails(3L))
            .thenReturn(Optional.of(order));

        ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () -> orderService.cancel(
                3L,
                authenticatedUser
            )
        );

        assertEquals(
            "You do not have permission to cancel this order", 
            exception.getMessage()
        );

        verify(orderRepository).findByIdWithDetails(3L);
    }
    
    @Test
    void mustDenySellerToListOrders() {
        User seller = new User();
        seller.setId(1L);
        seller.setRole(UserRole.SELLER);

        Pageable pageable = PageRequest.of(0, 10);

        ForbiddenException exception =
        assertThrows(ForbiddenException.class,
            () -> orderService.list(seller, pageable)
        );

        assertEquals(
            "Sellers cannot list orders",
            exception.getMessage()
        );

        verifyNoInteractions(orderRepository);
    }
    
    @Test
    void mustDenyCustomerToViewOthersOrder() {
        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setRole(UserRole.CUSTOMER);

        User otherUser = new User();
        otherUser.setId(2L);

        Order order = new Order();
        order.setId(3L);
        order.setCustomer(otherUser);

        when(orderRepository.findByIdWithDetails(3L))
            .thenReturn(Optional.of(order));

        ForbiddenException exception =
        assertThrows(
            ForbiddenException.class, 
            () -> orderService.findById(3L, authenticatedUser)
        );

        assertEquals(
            "You do not have permission to view this order", 
            exception.getMessage()
        );

        verify(orderRepository).findByIdWithDetails(3L);
    }
}
