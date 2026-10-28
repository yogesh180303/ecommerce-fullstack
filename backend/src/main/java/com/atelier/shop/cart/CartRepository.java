package com.atelier.shop.cart;

import com.atelier.shop.domain.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("select c from Cart c left join fetch c.items i left join fetch i.product where c.user.id = :userId")
    Optional<Cart> findWithItemsByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.user.id = :userId")
    Optional<Cart> findLockedByUserId(@Param("userId") Long userId);
}
