package com.ecommerce.projetobackend.order;

import com.ecommerce.projetobackend.product.Product;
import com.ecommerce.projetobackend.product.ProductRepository;
import com.ecommerce.projetobackend.product.ProductStatus;
import com.ecommerce.projetobackend.shared.exception.EntityNotFoundException;
import com.ecommerce.projetobackend.shared.exception.ForbiddenException;
import com.ecommerce.projetobackend.shared.exception.InsufficientStockException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderException;
import com.ecommerce.projetobackend.shared.exception.InvalidOrderStateException;
import com.ecommerce.projetobackend.user.User;
import com.ecommerce.projetobackend.user.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order create(OrderCreateRequest request, User authenticatedUser) {
        if (authenticatedUser.getRole() != UserRole.CUSTOMER) {
            throw new ForbiddenException("Only customers can create orders");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdWithSeller(itemRequest.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product", itemRequest.getProductId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new InvalidOrderException("Product " + itemRequest.getProductId() + " is not active");
            }
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Product " + itemRequest.getProductId() + " has insufficient stock");
            }

            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);

            BigDecimal unitPrice = product.getPrice();
            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .build();
            orderItems.add(item);
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        Order order = Order.builder()
                .customer(authenticatedUser)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        orderItems.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);
        orderRepository.flush();
        entityManager.refresh(saved);
        return orderRepository.findByIdWithDetails(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("Order", saved.getId()));
    }

    @Transactional
    public Order cancel(Long id, User authenticatedUser) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (authenticatedUser.getRole() != UserRole.ADMIN
                && !order.getCustomer().getId().equals(authenticatedUser.getId())) {
            throw new ForbiddenException("You do not have permission to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Cannot cancel order in status " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        orderRepository.flush();
        entityManager.refresh(order);
        return orderRepository.findByIdWithDetails(order.getId())
                .orElseThrow(() -> new EntityNotFoundException("Order", order.getId()));
    }

    public Page<Order> list(User authenticatedUser, Pageable pageable) {
        if (authenticatedUser.getRole() == UserRole.ADMIN) {
            return orderRepository.findAll(pageable);
        }
        if (authenticatedUser.getRole() == UserRole.CUSTOMER) {
            return orderRepository.findByCustomerId(authenticatedUser.getId(), pageable);
        }
        throw new ForbiddenException("Sellers cannot list orders");
    }

    public Order findById(Long id, User authenticatedUser) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Order", id));

        if (authenticatedUser.getRole() == UserRole.ADMIN) {
            return order;
        }
        if (order.getCustomer().getId().equals(authenticatedUser.getId())) {
            return order;
        }
        throw new ForbiddenException("You do not have permission to view this order");
    }
}
