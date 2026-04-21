package com.ecommerce.model;

import java.math.BigDecimal;

public class OrderItem {

    private Long id;
    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderItem() {}

    public OrderItem(Long id, Product product, Integer quantity, BigDecimal unitPrice) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return "OrderItem{id=" + id + ", product=" + product.getName() + ", quantity=" + quantity + ", unitPrice=" + unitPrice + "}";
    }
}
