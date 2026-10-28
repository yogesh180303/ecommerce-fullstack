package com.atelier.shop.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Cart() {}

    public Cart(User user) {
        this.user = user;
    }

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
        updatedAt = Instant.now();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
        updatedAt = Instant.now();
    }

    public void clearItems() {
        items.clear();
        updatedAt = Instant.now();
    }

    public void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public List<CartItem> getItems() { return items; }
}
