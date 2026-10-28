package com.atelier.shop.config;

import com.atelier.shop.category.CategoryRepository;
import com.atelier.shop.domain.Category;
import com.atelier.shop.domain.Product;
import com.atelier.shop.domain.Role;
import com.atelier.shop.domain.User;
import com.atelier.shop.product.ProductRepository;
import com.atelier.shop.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedLocalData(ProductRepository products, UserRepository users, CategoryRepository categories,
                                    PasswordEncoder encoder,
                                    @Value("${app.admin.email:}") String adminEmail,
                                    @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            if (!adminEmail.isBlank() && !adminPassword.isBlank() && !users.existsByEmailIgnoreCase(adminEmail)) {
                users.save(new User("Atelier Admin", adminEmail, encoder.encode(adminPassword), Role.ADMIN));
            }
            if (products.count() == 0) {
                products.saveAll(List.of(
                        new Product("Forma Ceramic Vase", "Home & Living", "Hand-thrown stoneware with a quiet, sculptural silhouette.", new BigDecimal("68.00"), "photo-1578500494198-246f612d3b3d", 18, 4.9, 124, "BESTSELLER", "sand"),
                        new Product("Everyday Linen Tote", "Accessories", "Washed European linen, cut for the everyday carry.", new BigDecimal("42.00"), "photo-1544816155-12df9643f363", 34, 4.8, 86, "NEW ARRIVAL", "rose"),
                        new Product("Cloud Knit Throw", "Textiles", "An easy, soft layer woven from recycled cotton.", new BigDecimal("115.00"), "photo-1600369671236-e74521d4b6ad", 9, 5.0, 57, null, "blue"),
                        new Product("Sunday Pour-over Set", "Kitchen", "A ceramic dripper and cup for slower mornings.", new BigDecimal("74.00"), "photo-1495474472287-4d71bcdd2085", 6, 4.9, 93, "LOW STOCK", "green"),
                        new Product("Arc Sculptural Lamp", "Home & Living", "A warm glow and a gentle curve, made for the corners you love.", new BigDecimal("189.00"), "photo-1507473885765-e6ed057f782c", 12, 4.7, 39, null, "lavender"),
                        new Product("Soft Form Candle", "Wellness", "Plant-based wax, cedar and hinoki. Poured in small batches.", new BigDecimal("36.00"), "photo-1603006905003-be475563bc59", 42, 4.9, 211, "MADE TO GIFT", "peach"),
                        new Product("Pebble Catchall", "Home & Living", "A small, useful place for the things you reach for daily.", new BigDecimal("48.00"), "photo-1618221195710-dd6b41faaea6", 20, 4.8, 62, null, "sand"),
                        new Product("Ribbed Glass Pair", "Kitchen", "Two hand-finished glasses for water, wine, or whatever.", new BigDecimal("52.00"), "photo-1514228742587-6b1558fcca3d", 16, 4.9, 78, null, "blue")
                ));
            }
            for (Product product : products.findAll()) {
                Category category = categories.findByNameIgnoreCase(product.getCategory())
                        .orElseGet(() -> categories.save(new Category(product.getCategory(), null)));
                if (product.getCategoryEntity() == null) product.setCategoryEntity(category);
            }
            products.flush();
        };
    }
}
