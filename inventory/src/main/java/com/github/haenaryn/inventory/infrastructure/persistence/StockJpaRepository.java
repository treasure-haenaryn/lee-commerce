package com.github.haenaryn.inventory.infrastructure.persistence;

import com.github.haenaryn.inventory.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface StockJpaRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductOptionId(Long productOptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productOptionId = :productOptionId")
    Optional<Stock> findByProductOptionIdForUpdate(@Param("productOptionId") Long productOptionId);
}
