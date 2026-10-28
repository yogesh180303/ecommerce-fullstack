package com.atelier.shop.category;

import com.atelier.shop.domain.Category;
import com.atelier.shop.product.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryController(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/categories/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return categories.findById(id).map(CategoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
        }
        return CategoryResponse.from(categories.save(new Category(name, normalizeDescription(request.description()))));
    }

    @PutMapping("/admin/categories/{id}")
    @Transactional
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        String oldName = category.getName();
        String newName = request.name().trim();
        if (!oldName.equalsIgnoreCase(newName) && categories.existsByNameIgnoreCase(newName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
        }
        category.update(newName, normalizeDescription(request.description()));
        products.renameCategory(oldName, newName);
        return CategoryResponse.from(category);
    }

    @DeleteMapping("/admin/categories/{id}")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if (products.countByCategoryIgnoreCase(category.getName()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Move or remove products before deleting this category");
        }
        categories.delete(category);
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    public record CategoryRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
    public record CategoryResponse(Long id, String name, String description, Instant createdAt) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.getCreatedAt());
        }
    }
}
