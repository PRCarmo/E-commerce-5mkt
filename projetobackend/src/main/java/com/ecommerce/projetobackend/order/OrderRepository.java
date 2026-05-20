package com.ecommerce.projetobackend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product p " +
           "LEFT JOIN FETCH p.seller " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "items", "items.product", "items.product.seller"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "items", "items.product", "items.product.seller"})
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
}
