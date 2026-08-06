package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    List<Product> findByNameContainingIgnoreCaseOrBarcodeContaining(String name, String barcode);

    @Query("select p from Product p where p.quantity <= p.lowStockThreshold order by p.quantity asc")
    List<Product> findLowStockProducts();

    @Query("select count(p) from Product p where p.quantity <= p.lowStockThreshold")
    long countLowStockProducts();

    @Query("select coalesce(sum(p.quantity * p.costPrice), 0) from Product p")
    java.math.BigDecimal calculateTotalStockValue();

    @Query("select distinct p.category from Product p where p.category is not null order by p.category")
    List<String> findAllCategories();
}
