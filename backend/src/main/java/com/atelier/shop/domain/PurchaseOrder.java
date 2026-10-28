package com.atelier.shop.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    @Column(nullable = false)
    private String status = "processing";
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected PurchaseOrder() {}

    public PurchaseOrder(User user, String address, BigDecimal total) {
        this.user = user;
        this.address = address;
        this.total = total;
    }

    public void addItem(OrderItem item) { items.add(item); item.setOrder(this); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getAddress() { return address; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setStatus(String status) { this.status = status; }
}
