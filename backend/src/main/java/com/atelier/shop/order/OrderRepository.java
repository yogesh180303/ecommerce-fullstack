package com.atelier.shop.order;

import com.atelier.shop.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @EntityGraph(attributePaths = {"items"})
    List<PurchaseOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    @EntityGraph(attributePaths = {"items", "user"})
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"items", "user"})
    Optional<PurchaseOrder> findByIdAndUserId(Long id, Long userId);
    @EntityGraph(attributePaths = {"user", "items"})
    List<PurchaseOrder> findTop5ByOrderByCreatedAtDesc();
}
