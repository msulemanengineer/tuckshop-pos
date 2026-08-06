package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findFirstByCashierUsernameAndStatusOrderByOpenedAtDesc(String cashierUsername, String status);
    List<Shift> findAllByOrderByOpenedAtDesc();
}
