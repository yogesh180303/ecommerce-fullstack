package com.atelier.shop.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PurchaseOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;
    @Column(nullable = false)
    private String productName;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItem() {}

    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.productName = product.getName();
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    public void setOrder(PurchaseOrder order) { this.order = order; }
    public Product getProduct() { return product; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
