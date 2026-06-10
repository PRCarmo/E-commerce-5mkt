package com.ecommerce.projetobackend.controllers;

import com.ecommerce.projetobackend.order.*;
import com.ecommerce.projetobackend.product.Product;
import com.ecommerce.projetobackend.config.SecurityConfig;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.shared.exception.InsufficientStockException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderException;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
public class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void mustCreateOrderSuccessfully() throws Exception {

        User customer = new User();
        customer.setId(1L);
        customer.setName("Test");
        customer.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        OrderItemRequest item =
                new OrderItemRequest(10L, 2);

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(item));

        Product product = new Product();
        product.setId(10L);
        product.setName("Test Product");

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(BigDecimal.valueOf(5000));

        Order order = new Order();
        order.setId(100L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(10000));
        order.setItems(List.of(orderItem));

        when(orderService.create(
                any(OrderCreateRequest.class),
                any(User.class)
        )).thenReturn(order);

        mockMvc.perform(
                post("/api/v1/orders")
                        .with(user(userDetails))
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.customerId").value(1))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.totalAmount").value(10000));
    }

    @Test
    void mustListOrdersSuccessfully() throws Exception {

        User customer = new User();
        customer.setId(1L);
        customer.setName("Pedro");

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        Order order = new Order();
        order.setId(100L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(500));

        Page<Order> page =
                new PageImpl<>(List.of(order));

        when(orderService.list(
                any(User.class),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(
                get("/api/v1/orders")
                        .with(user(userDetails))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id")
                .value(100))
        .andExpect(jsonPath("$.content[0].customerId")
                .value(1));
    }

    @Test
    void mustGetOrderByIdSuccessfully() throws Exception {

        User customer = new User();
        customer.setId(1L);
        customer.setName("Test");

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        Order order = new Order();
        order.setId(100L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.valueOf(500));

        when(orderService.findById(
                100L,
                customer
        )).thenReturn(order);

        mockMvc.perform(
                get("/api/v1/orders/100")
                        .with(user(userDetails))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
                .value(100))
        .andExpect(jsonPath("$.customerId")
                .value(1));
    }

    @Test
    void mustCancelOrderSuccessfully() throws Exception {

        User customer = new User();
        customer.setId(1L);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        Order order = new Order();
        order.setId(100L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderService.cancel(
                100L,
                customer
        )).thenReturn(order);

        mockMvc.perform(
                post("/api/v1/orders/100/cancel")
                        .with(user(userDetails))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status")
                .value("CANCELLED"));
    }

    @Test
    void mustRejectOrderWhenItemsIsEmpty() throws Exception {

        OrderCreateRequest request =
                new OrderCreateRequest(List.of());

        mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void mustRejectOrderWhenProductIdIsNull() throws Exception {

        OrderItemRequest item =
                new OrderItemRequest(null, 2);

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(item));

        mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void mustRejectOrderWhenQuantityIsInvalid() throws Exception {

        OrderItemRequest item =
                new OrderItemRequest(10L, 0);

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(item));

        mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(
                                org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void mustReturn404WhenOrderDoesNotExist() throws Exception {

        User customer = new User();
        customer.setId(1L);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        when(orderService.findById(
                eq(999L),
                any(User.class)
        ))
        .thenThrow(
                new EntityNotFoundException(
                        "Order",
                        999L
                )
        );

        mockMvc.perform(
                get("/api/v1/orders/999")
                        .with(user(userDetails))
        )
        .andExpect(status().isNotFound());
    }

    @Test
    void mustReturn403WhenCustomerViewsAnotherOrder() throws Exception {

        User customer = new User();
        customer.setId(1L);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        when(orderService.findById(
                eq(50L),
                any(User.class)
        ))
        .thenThrow(
                new ForbiddenException(
                        "You do not have permission to view this order"
                )
        );

        mockMvc.perform(
                get("/api/v1/orders/50")
                        .with(user(userDetails))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mustReturn403WhenCustomerCancelsAnotherUsersOrder() throws Exception {

        User customer = new User();
        customer.setId(1L);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        when(orderService.cancel(
                eq(50L),
                any(User.class)
        ))
        .thenThrow(
                new ForbiddenException(
                        "You do not have permission to cancel this order"
                )
        );

        mockMvc.perform(
                post("/api/v1/orders/50/cancel")
                        .with(user(userDetails))
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void mustReturn400WhenProductIsInactive() throws Exception {

        User customer = new User();
        customer.setId(1L);
        customer.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        OrderItemRequest item =
                new OrderItemRequest(10L, 2);

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(item));

        when(orderService.create(
                any(OrderCreateRequest.class),
                any(User.class)
        ))
        .thenThrow(
                new InvalidOrderException(
                        "Product 10 is not active"
                )
        );

        mockMvc.perform(
                post("/api/v1/orders")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());

        verify(orderService).create(
                any(OrderCreateRequest.class),
                any(User.class)
        );
    }

    @Test
    void mustReturn409WhenStockIsInsufficient() throws Exception {

        User customer = new User();
        customer.setId(1L);
        customer.setRole(UserRole.CUSTOMER);

        UserDetailsImpl userDetails =
                new UserDetailsImpl(customer);

        OrderItemRequest item =
                new OrderItemRequest(10L, 999);

        OrderCreateRequest request =
                new OrderCreateRequest(List.of(item));

        when(orderService.create(
                any(OrderCreateRequest.class),
                any(User.class)
        ))
        .thenThrow(
                new InsufficientStockException(
                        "Product 10 has insufficient stock"
                )
        );

        mockMvc.perform(
                post("/api/v1/orders")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isConflict());

        verify(orderService).create(
                any(OrderCreateRequest.class),
                any(User.class)
        );
    }
}
