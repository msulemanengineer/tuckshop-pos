package com.tuckshop.pos.repository;

import com.tuckshop.pos.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    interface TopSellerRow {
        String getProductName();
        Long getTotalQty();
    }

    @Query("select si.productName as productName, sum(si.quantity) as totalQty " +
           "from SaleItem si where si.sale.saleDate >= :start and si.sale.status = 'COMPLETED' " +
           "group by si.productName order by sum(si.quantity) desc")
    List<TopSellerRow> findTopSellers(@Param("start") LocalDateTime start);
}
