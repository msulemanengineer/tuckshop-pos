package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.SaleEditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleEditLogRepository extends JpaRepository<SaleEditLog, Long> {
    List<SaleEditLog> findAllByOrderByEditedAtDesc();
}
