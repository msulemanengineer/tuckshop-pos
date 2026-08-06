package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.dto.EditSaleItemRequest;
import com.tuckshop.pos.dto.VoidSaleRequest;
import com.tuckshop.pos.model.Sale;
import com.tuckshop.pos.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleApiController {

    private final SaleService saleService;

    public SaleApiController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public List<Sale> all() {
        return saleService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> byId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(saleService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    // Any logged-in user (cashier or owner) can attempt this, but it only succeeds
    // if the correct owner PIN is supplied - see SaleService.voidSale.
    @PostMapping("/{id}/void")
    public ResponseEntity<?> voidSale(@PathVariable Long id, @RequestBody VoidSaleRequest request) {
        try {
            return ResponseEntity.ok(saleService.voidSale(id, request.getReason(), request.getOwnerPin()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    // Any logged-in user can attempt this, but like void, it only succeeds with the
    // correct owner PIN - see SaleService.editSaleItemQuantity.
    @PostMapping("/{id}/items/{itemId}/edit")
    public ResponseEntity<?> editItem(@PathVariable Long id, @PathVariable Long itemId,
                                       @RequestBody EditSaleItemRequest request) {
        try {
            Sale sale = saleService.editSaleItemQuantity(id, itemId, request.getNewQuantity(),
                    request.getReason(), request.getOwnerPin());
            return ResponseEntity.ok(sale);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }
}
