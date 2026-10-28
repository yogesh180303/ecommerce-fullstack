package com.atelier.shop.product;

import com.atelier.shop.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.active = true and (:category is null or lower(p.category) = lower(:category)) and (:query is null or lower(p.name) like lower(concat('%', :query, '%')) or lower(p.description) like lower(concat('%', :query, '%'))) and (:minPrice is null or p.price >= :minPrice) and (:maxPrice is null or p.price <= :maxPrice)")
    Page<Product> search(@Param("category") String category, @Param("query") String query,
                         @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findLockedById(@Param("id") Long id);

    long countByCategoryIgnoreCase(String category);
    long countByActiveTrue();

    @Query("select coalesce(sum(p.stock), 0) from Product p where p.active = true")
    long countActiveInventory();

    @Modifying
    @Query("update Product p set p.category = :newName where lower(p.category) = lower(:oldName)")
    int renameCategory(@Param("oldName") String oldName, @Param("newName") String newName);
}
