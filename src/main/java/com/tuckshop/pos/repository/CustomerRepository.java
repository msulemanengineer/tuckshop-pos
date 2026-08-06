package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);

    @Query("select c from Customer c where c.currentBalance > 0 order by c.currentBalance desc")
    List<Customer> findAllWithOutstandingBalance();

    @Query("select coalesce(sum(c.currentBalance), 0) from Customer c")
    BigDecimal sumAllOutstandingBalances();
}
