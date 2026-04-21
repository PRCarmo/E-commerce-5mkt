package com.ecommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private Long id;
    private User customer;
    private List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public Order() {
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Order(Long id, User customer) {
        this();
        this.id = id;
        this.customer = customer;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", customer=" + customer.getName() + ", status=" + status + ", total=" + totalAmount + ", createdAt=" + createdAt + "}";
    }
}
