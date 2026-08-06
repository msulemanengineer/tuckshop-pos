package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.model.Product;
import com.tuckshop.pos.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    // Both roles can view/search products - a cashier needs this to bill customers
    @GetMapping
    public List<Product> all(@RequestParam(required = false) String q) {
        return productService.search(q);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> byId(@PathVariable Long id) {
        return productService.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("Product not found")));
    }

    @GetMapping("/barcode/{code}")
    public ResponseEntity<?> byBarcode(@PathVariable String code) {
        return productService.findByBarcode(code)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError("No product found for barcode " + code)));
    }

    @GetMapping("/low-stock")
    public List<Product> lowStock() {
        return productService.lowStockProducts();
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.categories();
    }

    // Everything below changes inventory data - owner only.
    // This is enforced here on the server, not just hidden in the UI, so a cashier
    // can never call these endpoints directly even by guessing the URL.

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.save(product));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.findById(id).<ResponseEntity<?>>map(existing -> {
            product.setId(id);
            return ResponseEntity.ok(productService.save(product));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Product not found")));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
