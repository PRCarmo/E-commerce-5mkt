package com.ecommerce.projetobackend.services;

import com.ecommerce.projetobackend.cart.AddCartItemRequest;
import com.ecommerce.projetobackend.cart.Cart;
import com.ecommerce.projetobackend.cart.CartItem;
import com.ecommerce.projetobackend.cart.CartRepository;
import com.ecommerce.projetobackend.cart.CartService;
import com.ecommerce.projetobackend.cart.UpdateCartItemRequest;
import com.ecommerce.projetobackend.order.Order;
import com.ecommerce.projetobackend.order.OrderCreateRequest;
import com.ecommerce.projetobackend.order.OrderService;
import com.ecommerce.projetobackend.product.Product;
import com.ecommerce.projetobackend.product.ProductRepository;
import com.ecommerce.projetobackend.product.ProductStatus;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CartService cartService;

    private User customer() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private Product activeProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(BigDecimal.valueOf(100));
        product.setStock(10);
        product.setStatus(ProductStatus.ACTIVE);
        return product;
    }

    @Test
    void mustDenyNonCustomer() {
        User seller = new User();
        seller.setId(1L);
        seller.setRole(UserRole.SELLER);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> cartService.getCart(seller)
        );

        assertEquals("Only customers can use the cart", exception.getMessage());
        verifyNoInteractions(cartRepository);
    }

    @Test
    void mustCreateCartOnFirstAccess() {
        User user = customer();
        Cart created = Cart.builder().id(5L).customer(user).build();

        when(cartRepository.findByCustomerId(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));

        Cart result = cartService.getCart(user);

        assertSame(created, result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void mustAddNewItemToCart() {
        User user = customer();
        Product product = activeProduct(10L);
        Cart cart = Cart.builder().id(5L).customer(user).items(new ArrayList<>()).build();

        when(productRepository.findByIdWithSeller(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        cartService.addItem(user, new AddCartItemRequest(10L, 2));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        Cart saved = captor.getValue();
        assertEquals(1, saved.getItems().size());
        assertEquals(2, saved.getItems().get(0).getQuantity());
        assertSame(product, saved.getItems().get(0).getProduct());
    }

    @Test
    void mustSumQuantityWhenItemAlreadyExists() {
        User user = customer();
        Product product = activeProduct(10L);
        CartItem existing = CartItem.builder().product(product).quantity(3).build();
        Cart cart = Cart.builder()
                .id(5L)
                .customer(user)
                .items(new ArrayList<>(List.of(existing)))
                .build();

        when(productRepository.findByIdWithSeller(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        cartService.addItem(user, new AddCartItemRequest(10L, 2));

        assertEquals(5, existing.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void mustRejectAddWhenProductIsInactive() {
        User user = customer();
        Product product = activeProduct(10L);
        product.setStatus(ProductStatus.INACTIVE);

        when(productRepository.findByIdWithSeller(10L)).thenReturn(Optional.of(product));

        InvalidOrderException exception = assertThrows(
                InvalidOrderException.class,
                () -> cartService.addItem(user, new AddCartItemRequest(10L, 2))
        );

        assertEquals("Product 10 is not active", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void mustRejectAddWhenProductDoesNotExist() {
        User user = customer();
        when(productRepository.findByIdWithSeller(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> cartService.addItem(user, new AddCartItemRequest(99L, 1))
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void mustUpdateItemQuantity() {
        User user = customer();
        Product product = activeProduct(10L);
        CartItem existing = CartItem.builder().product(product).quantity(3).build();
        Cart cart = Cart.builder()
                .id(5L)
                .customer(user)
                .items(new ArrayList<>(List.of(existing)))
                .build();

        when(productRepository.findByIdWithSeller(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        cartService.updateItem(user, 10L, new UpdateCartItemRequest(7));

        assertEquals(7, existing.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void mustThrowWhenUpdatingItemNotInCart() {
        User user = customer();
        Product product = activeProduct(10L);
        Cart cart = Cart.builder().id(5L).customer(user).items(new ArrayList<>()).build();

        when(productRepository.findByIdWithSeller(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        assertThrows(
                EntityNotFoundException.class,
                () -> cartService.updateItem(user, 10L, new UpdateCartItemRequest(2))
        );

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void mustRemoveItem() {
        User user = customer();
        Product product = activeProduct(10L);
        CartItem existing = CartItem.builder().product(product).quantity(3).build();
        Cart cart = Cart.builder()
                .id(5L)
                .customer(user)
                .items(new ArrayList<>(List.of(existing)))
                .build();

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        cartService.removeItem(user, 10L);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void mustClearCart() {
        User user = customer();
        Product product = activeProduct(10L);
        CartItem existing = CartItem.builder().product(product).quantity(3).build();
        Cart cart = Cart.builder()
                .id(5L)
                .customer(user)
                .items(new ArrayList<>(List.of(existing)))
                .build();

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        cartService.clear(user);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void mustCheckoutDelegatingToOrderService() {
        User user = customer();
        Product product = activeProduct(10L);
        CartItem existing = CartItem.builder().product(product).quantity(2).build();
        Cart cart = Cart.builder()
                .id(5L)
                .customer(user)
                .items(new ArrayList<>(List.of(existing)))
                .build();

        Order order = Order.builder().id(99L).customer(user).build();

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(orderService.create(any(OrderCreateRequest.class), any(User.class))).thenReturn(order);

        Order result = cartService.checkout(user);

        assertSame(order, result);
        assertTrue(cart.getItems().isEmpty());
        verify(orderService).create(any(OrderCreateRequest.class), any(User.class));
        verify(cartRepository).save(cart);
    }

    @Test
    void mustRejectCheckoutWhenCartIsEmpty() {
        User user = customer();
        Cart cart = Cart.builder().id(5L).customer(user).items(new ArrayList<>()).build();

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        InvalidOrderException exception = assertThrows(
                InvalidOrderException.class,
                () -> cartService.checkout(user)
        );

        assertEquals("Cart is empty", exception.getMessage());
        verifyNoInteractions(orderService);
    }
}
