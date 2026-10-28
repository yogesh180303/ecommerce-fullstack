package com.atelier.shop.admin;

import com.atelier.shop.category.CategoryRepository;
import com.atelier.shop.domain.PurchaseOrder;
import com.atelier.shop.order.OrderRepository;
import com.atelier.shop.product.ProductRepository;
import com.atelier.shop.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
public class AdminDashboardController {
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;
    private final CategoryRepository categories;

    public AdminDashboardController(ProductRepository products, OrderRepository orders,
                                    UserRepository users, CategoryRepository categories) {
        this.products = products;
        this.orders = orders;
        this.users = users;
        this.categories = categories;
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardResponse dashboard() {
        List<RecentOrder> recentOrders = orders.findTop5ByOrderByCreatedAtDesc().stream()
                .map(RecentOrder::from).toList();
        return new DashboardResponse(products.countByActiveTrue(), orders.count(), users.count(),
                categories.count(), products.countActiveInventory(), recentOrders);
    }

    public record DashboardResponse(long productCount, long orderCount, long userCount,
                                    long categoryCount, long inventoryUnits, List<RecentOrder> recentOrders) {}
    public record RecentOrder(Long id, String customerName, String status, BigDecimal total, Instant createdAt) {
        static RecentOrder from(PurchaseOrder order) {
            return new RecentOrder(order.getId(), order.getUser().getName(), order.getStatus(),
                    order.getTotal(), order.getCreatedAt());
        }
    }
}
