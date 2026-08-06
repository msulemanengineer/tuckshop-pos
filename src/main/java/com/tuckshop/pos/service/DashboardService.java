package com.tuckshop.pos.service;

import com.tuckshop.pos.dto.DashboardStatsDTO;
import com.tuckshop.pos.model.Product;
import com.tuckshop.pos.model.Sale;
import com.tuckshop.pos.repository.CreditTransactionRepository;
import com.tuckshop.pos.repository.ProductRepository;
import com.tuckshop.pos.repository.SaleItemRepository;
import com.tuckshop.pos.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public DashboardService(SaleRepository saleRepository, ProductRepository productRepository,
                             SaleItemRepository saleItemRepository,
                             CreditTransactionRepository creditTransactionRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEE");

    public DashboardStatsDTO buildStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal todaySales = saleRepository.sumTotalBetween(todayStart, now);
        // "Cash collected" = cash/card/wallet sales + khata payments received today.
        // Khata payments are real money in the drawer even though the original sale
        // wasn't a cash sale, so they have to be added in here.
        BigDecimal khataPaymentsToday = creditTransactionRepository.sumPaymentsBetween(todayStart, now);
        BigDecimal cashCollectedToday = saleRepository.sumCashCollectedBetween(todayStart, now).add(khataPaymentsToday);
        long todayTransactions = saleRepository.countBetween(todayStart, now);
        BigDecimal stockValue = productRepository.calculateTotalStockValue();
        long lowStockCount = productRepository.countLowStockProducts();

        BigDecimal todayProfit = saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(todayStart, now).stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .flatMap(s -> s.getItems().stream())
                .map(item -> item.getProfit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> weeklySales = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            BigDecimal total = saleRepository.sumTotalBetween(start, end);
            Map<String, Object> point = new HashMap<>();
            point.put("label", day.format(DAY_LABEL));
            point.put("total", total);
            weeklySales.add(point);
        }

        List<Map<String, Object>> topSellers = new ArrayList<>();
        saleItemRepository.findTopSellers(LocalDate.now().minusDays(7).atStartOfDay())
                .stream().limit(5)
                .forEach(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", row.getProductName());
                    m.put("qty", row.getTotalQty());
                    topSellers.add(m);
                });

        List<Map<String, Object>> recentSales = new ArrayList<>();
        for (Sale s : saleRepository.findTop10ByOrderBySaleDateDesc()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("itemCount", s.getItems().size());
            m.put("total", s.getTotalAmount());
            m.put("time", s.getSaleDate());
            m.put("paymentMethod", s.getPaymentMethod());
            recentSales.add(m);
        }

        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        for (Product p : productRepository.findLowStockProducts()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("quantity", p.getQuantity());
            m.put("threshold", p.getLowStockThreshold());
            lowStockItems.add(m);
        }

        return new DashboardStatsDTO(
                todaySales, cashCollectedToday, todayProfit, todayTransactions, stockValue, lowStockCount,
                weeklySales, topSellers, recentSales, lowStockItems
        );
    }
}
