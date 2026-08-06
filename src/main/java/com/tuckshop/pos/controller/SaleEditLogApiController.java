package com.tuckshop.pos.controller;

import com.tuckshop.pos.model.SaleEditLog;
import com.tuckshop.pos.repository.SaleEditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Owner-only - this is specifically the "did a cashier quietly edit a paid sale down"
// audit trail, so a cashier should never be able to see who's been edited and by how much.
@RestController
@RequestMapping("/api/sale-edits")
@PreAuthorize("hasRole('OWNER')")
public class SaleEditLogApiController {

    private final SaleEditLogRepository saleEditLogRepository;

    public SaleEditLogApiController(SaleEditLogRepository saleEditLogRepository) {
        this.saleEditLogRepository = saleEditLogRepository;
    }

    @GetMapping
    public List<SaleEditLog> all() {
        return saleEditLogRepository.findAllByOrderByEditedAtDesc();
    }
}
