package com.atelier.shop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category categoryEntity;
    @Column(nullable = false, length = 1200)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    private String image;
    @Column(nullable = false)
    private Integer stock = 0;
    private Double rating = 4.8;
    private Integer reviews = 0;
    private String badge;
    private String tone = "sand";
    @Column(nullable = false)
    private boolean active = true;

    protected Product() {}

    public Product(String name, String category, String description, BigDecimal price, String image, int stock, double rating, int reviews, String badge, String tone) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.image = image;
        this.stock = stock;
        this.rating = rating;
        this.reviews = reviews;
        this.badge = badge;
        this.tone = tone;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    @JsonIgnore
    public Category getCategoryEntity() { return categoryEntity; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getImage() { return image; }
    public Integer getStock() { return stock; }
    public Double getRating() { return rating; }
    public Integer getReviews() { return reviews; }
    public String getBadge() { return badge; }
    public String getTone() { return tone; }
    public boolean isActive() { return active; }
    public void update(String name, String category, String description, BigDecimal price, String image, Integer stock) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.image = image;
        this.stock = stock;
    }
    public void setCategoryEntity(Category categoryEntity) { this.categoryEntity = categoryEntity; }
    public void setStock(Integer stock) { this.stock = stock; }
    public void deactivate() { this.active = false; }
}
