package com.tuckshop.pos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cashierUsername;

    @Column(nullable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    private LocalDateTime closedAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal openingCash;

    @Column(precision = 10, scale = 2)
    private BigDecimal expectedClosingCash;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualClosingCash;

    @Column(precision = 10, scale = 2)
    private BigDecimal difference;

    @Column(nullable = false)
    private String status = "OPEN"; // OPEN or CLOSED

    public Shift() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(BigDecimal openingCash) {
        this.openingCash = openingCash;
    }

    public BigDecimal getExpectedClosingCash() {
        return expectedClosingCash;
    }

    public void setExpectedClosingCash(BigDecimal expectedClosingCash) {
        this.expectedClosingCash = expectedClosingCash;
    }

    public BigDecimal getActualClosingCash() {
        return actualClosingCash;
    }

    public void setActualClosingCash(BigDecimal actualClosingCash) {
        this.actualClosingCash = actualClosingCash;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
