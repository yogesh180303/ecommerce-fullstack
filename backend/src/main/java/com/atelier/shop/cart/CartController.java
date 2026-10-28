package com.atelier.shop.cart;

import com.atelier.shop.domain.Cart;
import com.atelier.shop.domain.CartItem;
import com.atelier.shop.domain.Product;
import com.atelier.shop.domain.User;
import com.atelier.shop.product.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartRepository carts;
    private final ProductRepository products;

    public CartController(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    @GetMapping
    @Transactional
    public CartResponse getCart(@AuthenticationPrincipal User user) {
        return response(getOrCreateCart(user));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CartResponse addItem(@AuthenticationPrincipal User user, @Valid @RequestBody AddItemRequest request) {
        Cart cart = getLockedCart(user);
        Product product = products.findById(request.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);
        int requestedQuantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        ensureStock(product, requestedQuantity);
        if (existing == null) cart.addItem(new CartItem(product, request.quantity()));
        else existing.setQuantity(requestedQuantity);
        cart.touch();
        return response(cart);
    }

    @PutMapping("/items/{itemId}")
    @Transactional
    public CartResponse updateItem(@AuthenticationPrincipal User user, @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateItemRequest request) {
        Cart cart = getLockedCart(user);
        CartItem item = findItem(cart, itemId);
        ensureStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        cart.touch();
        return response(cart);
    }

    @DeleteMapping("/items/{itemId}")
    @Transactional
    public CartResponse removeItem(@AuthenticationPrincipal User user, @PathVariable Long itemId) {
        Cart cart = getLockedCart(user);
        cart.removeItem(findItem(cart, itemId));
        return response(cart);
    }

    @DeleteMapping
    @Transactional
    public CartResponse clearCart(@AuthenticationPrincipal User user) {
        Cart cart = getLockedCart(user);
        cart.clearItems();
        return response(cart);
    }

    private Cart getOrCreateCart(User user) {
        return carts.findWithItemsByUserId(user.getId()).orElseGet(() -> carts.save(new Cart(user)));
    }

    private Cart getLockedCart(User user) {
        return carts.findLockedByUserId(user.getId())
                .orElseGet(() -> carts.save(new Cart(user)));
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(item -> item.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, product.getName() + " does not have enough stock");
        }
    }

    private CartResponse response(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, subtotal, items.stream().mapToInt(CartItemResponse::quantity).sum());
    }

    public record AddItemRequest(@NotNull Long productId, @NotNull @Min(1) @Max(10000) Integer quantity) {}
    public record UpdateItemRequest(@NotNull @Min(1) @Max(10000) Integer quantity) {}
    public record CartItemResponse(Long id, Long productId, String name, String category, String image,
                                   BigDecimal price, int quantity, int stock, BigDecimal subtotal) {
        static CartItemResponse from(CartItem item) {
            Product product = item.getProduct();
            return new CartItemResponse(item.getId(), product.getId(), product.getName(), product.getCategory(),
                    product.getImage(), product.getPrice(), item.getQuantity(), product.getStock(),
                    product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }
    public record CartResponse(List<CartItemResponse> items, BigDecimal subtotal, int itemCount) {}
}
