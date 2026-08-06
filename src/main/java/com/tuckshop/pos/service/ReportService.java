package com.tuckshop.pos.service;

import com.tuckshop.pos.model.CreditTransaction;
import com.tuckshop.pos.model.Sale;
import com.tuckshop.pos.model.SaleItem;
import com.tuckshop.pos.repository.CreditTransactionRepository;
import com.tuckshop.pos.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public ReportService(SaleRepository saleRepository, CreditTransactionRepository creditTransactionRepository) {
        this.saleRepository = saleRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    public Map<String, Object> buildReport(LocalDate from, LocalDate to, Long customerId, String paymentMethod) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Sale> sales = saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(start, end).stream()
                .filter(s -> !"VOIDED".equals(s.getStatus()))
                .filter(s -> customerId == null || (s.getCustomer() != null && s.getCustomer().getId().equals(customerId)))
                .filter(s -> paymentMethod == null || paymentMethod.isBlank() || paymentMethod.equalsIgnoreCase(s.getPaymentMethod()))
                .toList();

        BigDecimal totalRevenue = sales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cash collected = non-khata sales + khata payments actually received in this
        // range (a customer paying off what they owe is real cash in the drawer)
        BigDecimal khataPayments = creditTransactionRepository.findPaymentsBetween(start, end).stream()
                .filter(t -> customerId == null || (t.getCustomer() != null && t.getCustomer().getId().equals(customerId)))
                .map(CreditTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashCollected = sales.stream()
                .filter(s -> !"KHATA".equals(s.getPaymentMethod()))
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(khataPayments);
        long totalTransactions = sales.size();
        long totalItemsSold = sales.stream()
                .flatMap(s -> s.getItems().stream())
                .mapToLong(SaleItem::getQuantity)
                .sum();

        // sales by day
        Map<LocalDate, BigDecimal> byDayMap = new TreeMap<>();
        for (Sale s : sales) {
            LocalDate day = s.getSaleDate().toLocalDate();
            byDayMap.merge(day, s.getTotalAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> byDay = new ArrayList<>();
        byDayMap.forEach((day, total) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day.toString());
            m.put("total", total);
            byDay.add(m);
        });

        // top products by quantity sold, revenue, cost, and profit
        Map<String, long[]> qtyByProduct = new HashMap<>(); // name -> [qty]
        Map<String, BigDecimal> revenueByProduct = new HashMap<>();
        Map<String, BigDecimal> costByProduct = new HashMap<>();
        Map<String, BigDecimal> profitByProduct = new HashMap<>();
        for (Sale s : sales) {
            for (SaleItem item : s.getItems()) {
                qtyByProduct.merge(item.getProductName(), new long[]{item.getQuantity()},
                        (a, b) -> new long[]{a[0] + b[0]});
                revenueByProduct.merge(item.getProductName(), item.getSubtotal(), BigDecimal::add);
                BigDecimal costValue = item.getUnitCost() == null ? BigDecimal.ZERO
                        : item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity()));
                costByProduct.merge(item.getProductName(), costValue, BigDecimal::add);
                profitByProduct.merge(item.getProductName(), item.getProfit(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> topProducts = qtyByProduct.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("qty", e.getValue()[0]);
                    m.put("costValue", costByProduct.get(e.getKey()));
                    m.put("revenue", revenueByProduct.get(e.getKey()));
                    m.put("profit", profitByProduct.get(e.getKey()));
                    return m;
                })
                .collect(Collectors.toList());

        BigDecimal totalProfit = profitByProduct.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCostValue = costByProduct.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // by payment method
        Map<String, BigDecimal> byMethodMap = new TreeMap<>();
        for (Sale s : sales) {
            byMethodMap.merge(s.getPaymentMethod(), s.getTotalAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> byPaymentMethod = new ArrayList<>();
        byMethodMap.forEach((method, total) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("method", method);
            m.put("total", total);
            byPaymentMethod.add(m);
        });

        // by customer (khata sales only, unless a specific customer filter is already applied)
        Map<String, BigDecimal> byCustomerMap = new TreeMap<>();
        for (Sale s : sales) {
            if (s.getCustomer() != null) {
                String name = s.getCustomerNameSnapshot() != null ? s.getCustomerNameSnapshot() : "Customer #" + s.getCustomer().getId();
                byCustomerMap.merge(name, s.getTotalAmount(), BigDecimal::add);
            }
        }
        List<Map<String, Object>> byCustomer = new ArrayList<>();
        byCustomerMap.forEach((name, total) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("customer", name);
            m.put("total", total);
            byCustomer.add(m);
        });

        // raw transaction list for the detail table
        List<Map<String, Object>> transactions = new ArrayList<>();
        for (Sale s : sales) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("date", s.getSaleDate());
            m.put("itemCount", s.getItems().size());
            m.put("total", s.getTotalAmount());
            m.put("paymentMethod", s.getPaymentMethod());
            m.put("cashier", s.getCashierUsername());
            m.put("customer", s.getCustomerNameSnapshot());
            transactions.add(m);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("from", from.toString());
        report.put("to", to.toString());
        report.put("totalRevenue", totalRevenue);
        report.put("cashCollected", cashCollected);
        report.put("totalCostValue", totalCostValue);
        report.put("totalProfit", totalProfit);
        report.put("totalTransactions", totalTransactions);
        report.put("totalItemsSold", totalItemsSold);
        report.put("byDay", byDay);
        report.put("topProducts", topProducts);
        report.put("byPaymentMethod", byPaymentMethod);
        report.put("byCustomer", byCustomer);
        report.put("transactions", transactions);
        return report;
    }

    public String toCsv(LocalDate from, LocalDate to, Long customerId, String paymentMethod) {
        Map<String, Object> report = buildReport(from, to, customerId, paymentMethod);
        StringBuilder sb = new StringBuilder();

        sb.append("SUMMARY\n");
        sb.append("Period,").append(report.get("from")).append(" to ").append(report.get("to")).append("\n");
        sb.append("Total revenue,").append(report.get("totalRevenue")).append("\n");
        sb.append("Cash collected,").append(report.get("cashCollected")).append("\n");
        sb.append("Total cost value,").append(report.get("totalCostValue")).append("\n");
        sb.append("Total profit,").append(report.get("totalProfit")).append("\n");
        sb.append("Transactions,").append(report.get("totalTransactions")).append("\n\n");

        sb.append("PRODUCT PROFITABILITY\n");
        sb.append("Product,Qty sold,Cost value,Selling value,Profit\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topProducts = (List<Map<String, Object>>) report.get("topProducts");
        for (Map<String, Object> p : topProducts) {
            sb.append(p.get("name")).append(",")
              .append(p.get("qty")).append(",")
              .append(p.get("costValue")).append(",")
              .append(p.get("revenue")).append(",")
              .append(p.get("profit")).append("\n");
        }
        sb.append("\n");

        sb.append("TRANSACTIONS\n");
        sb.append("Sale ID,Date,Items,Total,Payment method,Cashier,Customer\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) report.get("transactions");
        for (Map<String, Object> t : transactions) {
            sb.append(t.get("id")).append(",")
              .append(t.get("date")).append(",")
              .append(t.get("itemCount")).append(",")
              .append(t.get("total")).append(",")
              .append(t.get("paymentMethod")).append(",")
              .append(t.get("cashier")).append(",")
              .append(t.get("customer") == null ? "" : t.get("customer"))
              .append("\n");
        }
        return sb.toString();
    }
}
