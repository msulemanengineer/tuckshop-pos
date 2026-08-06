package com.tuckshop.pos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardStatsDTO {
    private BigDecimal todaySales;
    private BigDecimal cashCollectedToday;
    private BigDecimal todayProfit;
    private long todayTransactions;
    private BigDecimal stockValue;
    private long lowStockCount;
    private List<Map<String, Object>> weeklySales;
    private List<Map<String, Object>> topSellers;
    private List<Map<String, Object>> recentSales;
    private List<Map<String, Object>> lowStockItems;

    public DashboardStatsDTO(BigDecimal todaySales, BigDecimal cashCollectedToday, BigDecimal todayProfit,
                              long todayTransactions, BigDecimal stockValue, long lowStockCount,
                              List<Map<String, Object>> weeklySales, List<Map<String, Object>> topSellers,
                              List<Map<String, Object>> recentSales, List<Map<String, Object>> lowStockItems) {
        this.todaySales = todaySales;
        this.cashCollectedToday = cashCollectedToday;
        this.todayProfit = todayProfit;
        this.todayTransactions = todayTransactions;
        this.stockValue = stockValue;
        this.lowStockCount = lowStockCount;
        this.weeklySales = weeklySales;
        this.topSellers = topSellers;
        this.recentSales = recentSales;
        this.lowStockItems = lowStockItems;
    }

    public BigDecimal getTodaySales() {
        return todaySales;
    }

    public BigDecimal getCashCollectedToday() {
        return cashCollectedToday;
    }

    public BigDecimal getTodayProfit() {
        return todayProfit;
    }

    public long getTodayTransactions() {
        return todayTransactions;
    }

    public BigDecimal getStockValue() {
        return stockValue;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public List<Map<String, Object>> getWeeklySales() {
        return weeklySales;
    }

    public List<Map<String, Object>> getTopSellers() {
        return topSellers;
    }

    public List<Map<String, Object>> getRecentSales() {
        return recentSales;
    }

    public List<Map<String, Object>> getLowStockItems() {
        return lowStockItems;
    }
}
