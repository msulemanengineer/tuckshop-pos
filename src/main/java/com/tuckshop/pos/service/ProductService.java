package com.tuckshop.pos.service;

import com.tuckshop.pos.model.Product;
import com.tuckshop.pos.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ActivityLogService activityLogService;

    public ProductService(ProductRepository productRepository, ActivityLogService activityLogService) {
        this.productRepository = productRepository;
        this.activityLogService = activityLogService;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    public List<Product> search(String term) {
        if (term == null || term.isBlank()) {
            return productRepository.findAll();
        }
        return productRepository.findByNameContainingIgnoreCaseOrBarcodeContaining(term, term);
    }

    public List<Product> lowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public List<String> categories() {
        return productRepository.findAllCategories();
    }

    public Product save(Product product) {
        boolean isNew = product.getId() == null;
        if (isNew && productRepository.existsByBarcode(product.getBarcode())) {
            throw new IllegalArgumentException("A product with this barcode already exists.");
        }

        if (!isNew) {
            productRepository.findById(product.getId()).ifPresent(existing -> {
                if (existing.getSellingPrice().compareTo(product.getSellingPrice()) != 0) {
                    activityLogService.log("PRICE_CHANGED", product.getName() + ": Rs "
                            + existing.getSellingPrice() + " -> Rs " + product.getSellingPrice());
                }
            });
        }

        Product saved = productRepository.save(product);
        activityLogService.log(isNew ? "PRODUCT_ADDED" : "PRODUCT_EDITED", saved.getName());
        return saved;
    }

    public void delete(Long id) {
        productRepository.findById(id).ifPresent(p ->
                activityLogService.log("PRODUCT_DELETED", p.getName() + " (barcode " + p.getBarcode() + ")"));
        productRepository.deleteById(id);
    }

    public Product adjustStock(Long productId, int delta) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        int newQty = p.getQuantity() + delta;
        if (newQty < 0) {
            throw new IllegalStateException("Not enough stock for " + p.getName());
        }
        p.setQuantity(newQty);
        return productRepository.save(p);
    }
}
