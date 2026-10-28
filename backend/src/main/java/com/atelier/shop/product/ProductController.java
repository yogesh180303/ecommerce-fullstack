package com.atelier.shop.product;

import com.atelier.shop.domain.Product;
import com.atelier.shop.domain.Category;
import com.atelier.shop.category.CategoryRepository;
import com.atelier.shop.order.OrderItemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ProductController {
    private final ProductRepository products;
    private final OrderItemRepository orderItems;
    private final CategoryRepository categories;

    public ProductController(ProductRepository products, OrderItemRepository orderItems, CategoryRepository categories) {
        this.products = products;
        this.orderItems = orderItems;
        this.categories = categories;
    }

    @GetMapping("/api/products")
    public PageResponse<Product> search(@RequestParam(required = false) String category,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(required = false) BigDecimal minPrice,
                                        @RequestParam(required = false) BigDecimal maxPrice,
                                        @RequestParam(defaultValue = "id") String sortBy,
                                        @RequestParam(defaultValue = "asc") String direction,
                                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid price range");
        }
        if (!List.of("id", "name", "price").contains(sortBy) || !List.of("asc", "desc").contains(direction.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field or direction");
        }
        String categoryFilter = category;
        if (categoryId != null) {
            categoryFilter = categories.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"))
                    .getName();
        }
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        String searchTerm = search != null ? search : q;
        return PageResponse.from(products.search(blankToNull(categoryFilter), blankToNull(searchTerm), minPrice, maxPrice,
                PageRequest.of(page, size, sort)));
    }

    @GetMapping("/api/products/{id}")
    public Product get(@PathVariable Long id) {
        return products.findById(id).filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @PostMapping("/api/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        String categoryName = request.category().trim();
        Category category = categories.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> categories.save(new Category(categoryName, null)));
        Product product = new Product(request.name().trim(), categoryName, request.description().trim(),
                request.price(), request.image(), request.stock(), 4.8, 0, null, "sand");
        product.setCategoryEntity(category);
        return products.save(product);
    }

    @PutMapping("/api/admin/products/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product current = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        String categoryName = request.category().trim();
        Category category = categories.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> categories.save(new Category(categoryName, null)));
        current.update(request.name().trim(), categoryName, request.description().trim(),
                request.price(), request.image(), request.stock());
        current.setCategoryEntity(category);
        return products.save(current);
    }

    @DeleteMapping("/api/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (orderItems.existsByProductId(id)) {
            product.deactivate();
            products.save(product);
            return;
        }
        products.deleteById(id);
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record ProductRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 100) String category,
                                 @NotBlank @Size(max = 1200) String description, @NotNull @DecimalMin("0.01") BigDecimal price,
                                 @Size(max = 1000) String image, @NotNull @Min(0) Integer stock) {}
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        static <T> PageResponse<T> from(Page<T> result) {
            return new PageResponse<>(result.getContent(), result.getNumber(), result.getSize(),
                    result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
        }
    }
}
