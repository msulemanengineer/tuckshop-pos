package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.dto.CheckoutRequest;
import com.tuckshop.pos.model.Sale;
import com.tuckshop.pos.service.SaleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pos")
public class PosApiController {

    private final SaleService saleService;

    public PosApiController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        try {
            Sale sale = saleService.checkout(request);
            return ResponseEntity.ok(sale);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(e.getMessage()));
        }
    }
}
