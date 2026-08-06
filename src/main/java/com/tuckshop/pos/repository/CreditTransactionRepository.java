package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    List<CreditTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Khata payments received are real cash in the drawer - needed alongside cash
    // sales to get an accurate "cash collected" figure
    @Query("select coalesce(sum(t.amount), 0) from CreditTransaction t where t.type = 'PAYMENT' " +
           "and t.createdAt >= :start and t.createdAt < :end")
    BigDecimal sumPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select t from CreditTransaction t where t.type = 'PAYMENT' and t.createdAt >= :start and t.createdAt < :end")
    List<CreditTransaction> findPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select t from CreditTransaction t where t.type = 'PAYMENT' and t.recordedBy = :username " +
           "and t.createdAt >= :start and t.createdAt < :end")
    List<CreditTransaction> findPaymentsByUserBetween(@Param("username") String username,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
}
