package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findBySaleDateBetweenOrderBySaleDateDesc(LocalDateTime start, LocalDateTime end);

    List<Sale> findTop10ByOrderBySaleDateDesc();

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.saleDate >= :start and s.saleDate < :end and s.status = 'COMPLETED'")
    BigDecimal sumTotalBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Excludes KHATA sales - those are owed, not cash actually collected today
    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.saleDate >= :start and s.saleDate < :end and s.status = 'COMPLETED' and s.paymentMethod != 'KHATA'")
    BigDecimal sumCashCollectedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(s) from Sale s where s.saleDate >= :start and s.saleDate < :end and s.status = 'COMPLETED'")
    long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
