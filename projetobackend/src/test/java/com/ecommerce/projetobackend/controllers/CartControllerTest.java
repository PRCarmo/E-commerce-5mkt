package com.ecommerce.projetobackend.controllers;

import com.ecommerce.projetobackend.cart.AddCartItemRequest;
import com.ecommerce.projetobackend.cart.Cart;
import com.ecommerce.projetobackend.cart.CartController;
import com.ecommerce.projetobackend.cart.CartItem;
import com.ecommerce.projetobackend.cart.CartService;
import com.ecommerce.projetobackend.cart.UpdateCartItemRequest;
import com.ecommerce.projetobackend.config.SecurityConfig;
import com.ecommerce.projetobackend.order.Order;
import com.ecommerce.projetobackend.order.OrderStatus;
import com.ecommerce.projetobackend.product.Product;
import com.ecommerce.projetobackend.security.UserDetailsImpl;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.support.WebSecurityTestConfig;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, WebSecurityTestConfig.class})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    private UserDetailsImpl customerDetails() {
        User user = new User();
        user.setId(1L);
        user.setName("Customer");
        user.setRole(UserRole.CUSTOMER);
        return new UserDetailsImpl(user);
    }

    private Cart sampleCart() {
        Product product = new Product();
        product.setId(10L);
        product.setName("Notebook");
        product.setPrice(BigDecimal.valueOf(100));

        CartItem item = CartItem.builder()
                .product(product)
                .quantity(2)
                .build();

        return Cart.builder()
                .id(5L)
                .customer(customerDetails().getUser())
                .items(new ArrayList<>(List.of(item)))
                .build();
    }

    @Test
    void mustGetCartSuccessfully() throws Exception {
        when(cartService.getCart(any(User.class))).thenReturn(sampleCart());

        mockMvc.perform(
                        get("/api/v1/cart")
                                .with(user(customerDetails())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(200))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].subtotal").value(200));
    }

    @Test
    void mustAddItemSuccessfully() throws Exception {
        when(cartService.addItem(any(User.class), any(AddCartItemRequest.class)))
                .thenReturn(sampleCart());

        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(user(customerDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AddCartItemRequest(10L, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(10));

        verify(cartService).addItem(any(User.class), any(AddCartItemRequest.class));
    }

    @Test
    void mustRejectAddWhenProductIdIsNull() throws Exception {
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(user(customerDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AddCartItemRequest(null, 2))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void mustRejectAddWhenQuantityIsInvalid() throws Exception {
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(user(customerDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AddCartItemRequest(10L, 0))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void mustUpdateItemSuccessfully() throws Exception {
        when(cartService.updateItem(any(User.class), eq(10L), any(UpdateCartItemRequest.class)))
                .thenReturn(sampleCart());

        mockMvc.perform(
                        put("/api/v1/cart/items/10")
                                .with(user(customerDetails()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(3))))
                .andExpect(status().isOk());

        verify(cartService).updateItem(any(User.class), eq(10L), any(UpdateCartItemRequest.class));
    }

    @Test
    void mustRemoveItemSuccessfully() throws Exception {
        when(cartService.removeItem(any(User.class), eq(10L))).thenReturn(sampleCart());

        mockMvc.perform(
                        delete("/api/v1/cart/items/10")
                                .with(user(customerDetails())))
                .andExpect(status().isNoContent());

        verify(cartService).removeItem(any(User.class), eq(10L));
    }

    @Test
    void mustClearCartSuccessfully() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/cart")
                                .with(user(customerDetails())))
                .andExpect(status().isNoContent());

        verify(cartService).clear(any(User.class));
    }

    @Test
    void mustCheckoutSuccessfully() throws Exception {
        Order order = Order.builder()
                .id(99L)
                .customer(customerDetails().getUser())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200))
                .items(new ArrayList<>())
                .build();

        when(cartService.checkout(any(User.class))).thenReturn(order);

        mockMvc.perform(
                        post("/api/v1/cart/checkout")
                                .with(user(customerDetails())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void mustReturn403WhenServiceDeniesNonCustomer() throws Exception {
        when(cartService.getCart(any(User.class)))
                .thenThrow(new ForbiddenException("Only customers can use the cart"));

        mockMvc.perform(
                        get("/api/v1/cart")
                                .with(user(customerDetails())))
                .andExpect(status().isForbidden());
    }
}
