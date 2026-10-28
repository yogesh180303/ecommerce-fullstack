package com.atelier.shop.order;

import com.atelier.shop.cart.CartRepository;
import com.atelier.shop.domain.*;
import com.atelier.shop.product.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@RestController
public class OrderController {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final CartRepository carts;

    public OrderController(OrderRepository orders, ProductRepository products, CartRepository carts) {
        this.orders = orders;
        this.products = products;
        this.carts = carts;
    }

    @GetMapping("/api/orders")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal User user) {
        return orders.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse myOrder(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return orders.findByIdAndUserId(id, user.getId()).map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderResponse create(@AuthenticationPrincipal User user, @Valid @RequestBody CreateOrderRequest request) {
        return placeOrder(user, request.address(), request.items());
    }

    @PostMapping("/api/orders/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderResponse checkout(@AuthenticationPrincipal User user, @Valid @RequestBody CheckoutRequest request) {
        Cart cart = carts.findLockedByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty"));
        List<OrderLine> lines = cart.getItems().stream()
                .map(item -> new OrderLine(item.getProduct().getId(), item.getQuantity())).toList();
        if (lines.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        OrderResponse response = placeOrder(user, request.address(), lines);
        cart.clearItems();
        return response;
    }

    @GetMapping("/api/admin/orders")
    public List<OrderResponse> allOrders() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    @Transactional
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        PurchaseOrder order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        String currentStatus = order.getStatus();
        String nextStatus = request.status().toLowerCase();
        boolean allowed = switch (currentStatus) {
            case "processing" -> List.of("shipped", "cancelled").contains(nextStatus);
            case "shipped" -> "delivered".equals(nextStatus);
            default -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "That order status transition is not allowed");
        if ("cancelled".equals(nextStatus)) {
            Map<Long, Integer> quantitiesToRestock = new LinkedHashMap<>();
            for (OrderItem item : order.getItems()) {
                quantitiesToRestock.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
            }
            for (Long productId : new TreeSet<>(quantitiesToRestock.keySet())) {
                Product product = products.findLockedById(productId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "An ordered product is no longer available"));
                product.setStock(product.getStock() + quantitiesToRestock.get(productId));
            }
        }
        order.setStatus(nextStatus);
        return OrderResponse.from(order);
    }

    private OrderResponse placeOrder(User user, String address, List<OrderLine> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : lines) quantities.merge(line.productId(), line.quantity(), Integer::sum);

        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        for (Long productId : new TreeSet<>(quantities.keySet())) {
            Product product = products.findLockedById(productId)
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A product in your bag is no longer available"));
            int quantity = quantities.get(productId);
            if (product.getStock() < quantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, product.getName() + " does not have enough stock");
            }
            lockedProducts.put(productId, product);
        }

        PurchaseOrder order = new PurchaseOrder(user, address.trim(), BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            int quantity = entry.getValue();
            product.setStock(product.getStock() - quantity);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order.addItem(new OrderItem(product, quantity));
        }
        order.setTotal(total);
        PurchaseOrder saved = orders.save(order);
        return OrderResponse.from(saved);
    }

    public record CreateOrderRequest(@NotBlank @Size(max = 500) String address,
                                     @NotEmpty @Size(max = 100) List<@Valid OrderLine> items) {}
    public record CheckoutRequest(@NotBlank @Size(max = 500) String address) {}
    public record OrderLine(@NotNull Long productId, @NotNull @Min(1) @Max(10000) Integer quantity) {}
    public record StatusRequest(@NotBlank @Pattern(regexp = "(?i)shipped|delivered|cancelled") String status) {}
    public record OrderItemResponse(String productName, Integer quantity, BigDecimal unitPrice) {
        static OrderItemResponse from(OrderItem item) { return new OrderItemResponse(item.getProductName(), item.getQuantity(), item.getUnitPrice()); }
    }
    public record OrderResponse(Long id, String status, String address, BigDecimal total, Instant createdAt, List<OrderItemResponse> items) {
        static OrderResponse from(PurchaseOrder order) {
            return new OrderResponse(order.getId(), order.getStatus(), order.getAddress(), order.getTotal(), order.getCreatedAt(),
                    order.getItems().stream().map(OrderItemResponse::from).toList());
        }
    }
}
