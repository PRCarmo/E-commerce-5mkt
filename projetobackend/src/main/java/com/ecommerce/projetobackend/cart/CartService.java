package com.ecommerce.projetobackend.cart;

import com.ecommerce.projetobackend.order.Order;
import com.ecommerce.projetobackend.order.OrderCreateRequest;
import com.ecommerce.projetobackend.order.OrderItemRequest;
import com.ecommerce.projetobackend.order.OrderService;
import com.ecommerce.projetobackend.product.Product;
import com.ecommerce.projetobackend.product.ProductRepository;
import com.ecommerce.projetobackend.product.ProductStatus;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       OrderService orderService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
    }

    public Cart getCart(User authenticatedUser) {
        ensureCustomer(authenticatedUser);
        return getOrCreateCart(authenticatedUser);
    }

    public Cart addItem(User authenticatedUser, AddCartItemRequest request) {
        ensureCustomer(authenticatedUser);
        Product product = loadActiveProduct(request.getProductId());
        Cart cart = getOrCreateCart(authenticatedUser);

        CartItem existing = findItem(cart, product.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
        }

        cartRepository.save(cart);
        return reload(authenticatedUser);
    }

    public Cart updateItem(User authenticatedUser, Long productId, UpdateCartItemRequest request) {
        ensureCustomer(authenticatedUser);
        loadActiveProduct(productId);
        Cart cart = getOrCreateCart(authenticatedUser);

        CartItem item = findItem(cart, productId);
        if (item == null) {
            throw new EntityNotFoundException("CartItem", productId);
        }
        item.setQuantity(request.getQuantity());

        cartRepository.save(cart);
        return reload(authenticatedUser);
    }

    public Cart removeItem(User authenticatedUser, Long productId) {
        ensureCustomer(authenticatedUser);
        Cart cart = getOrCreateCart(authenticatedUser);

        CartItem item = findItem(cart, productId);
        if (item == null) {
            throw new EntityNotFoundException("CartItem", productId);
        }
        cart.getItems().remove(item);

        cartRepository.save(cart);
        return reload(authenticatedUser);
    }

    public void clear(User authenticatedUser) {
        ensureCustomer(authenticatedUser);
        cartRepository.findByCustomerId(authenticatedUser.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    @Transactional
    public Order checkout(User authenticatedUser) {
        ensureCustomer(authenticatedUser);
        Cart cart = cartRepository.findByCustomerId(authenticatedUser.getId())
                .orElseThrow(() -> new InvalidOrderException("Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new InvalidOrderException("Cart is empty");
        }

        List<OrderItemRequest> orderItems = cart.getItems().stream()
                .map(item -> new OrderItemRequest(item.getProduct().getId(), item.getQuantity()))
                .toList();

        Order order = orderService.create(new OrderCreateRequest(orderItems), authenticatedUser);

        cart.getItems().clear();
        cartRepository.save(cart);
        return order;
    }

    private Cart getOrCreateCart(User authenticatedUser) {
        return cartRepository.findByCustomerId(authenticatedUser.getId())
                .orElseGet(() -> {
                    cartRepository.save(Cart.builder().customer(authenticatedUser).build());
                    return reload(authenticatedUser);
                });
    }

    private Cart reload(User authenticatedUser) {
        return cartRepository.findByCustomerId(authenticatedUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cart", authenticatedUser.getId()));
    }

    private Product loadActiveProduct(Long productId) {
        Product product = productRepository.findByIdWithSeller(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new InvalidOrderException("Product " + productId + " is not active");
        }
        return product;
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void ensureCustomer(User authenticatedUser) {
        if (authenticatedUser.getRole() != UserRole.CUSTOMER) {
            throw new ForbiddenException("Only customers can use the cart");
        }
    }
}
