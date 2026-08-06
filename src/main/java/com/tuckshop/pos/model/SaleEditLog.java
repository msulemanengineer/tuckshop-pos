package com.tuckshop.pos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A structured record of every quantity correction made to an already-completed sale.
 * Kept separate from the general ActivityLog so the owner has one focused page to scan
 * for the pattern that actually matters here: a cashier repeatedly editing sales down
 * after payment was already collected.
 */
@Entity
@Table(name = "sale_edit_logs")
public class SaleEditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long saleId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String cashierUsername;

    @Column(nullable = false)
    private Integer oldQuantity;

    @Column(nullable = false)
    private Integer newQuantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal oldSubtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal newSubtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountRemoved;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime editedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }

    public Integer getOldQuantity() {
        return oldQuantity;
    }

    public void setOldQuantity(Integer oldQuantity) {
        this.oldQuantity = oldQuantity;
    }

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    public BigDecimal getOldSubtotal() {
        return oldSubtotal;
    }

    public void setOldSubtotal(BigDecimal oldSubtotal) {
        this.oldSubtotal = oldSubtotal;
    }

    public BigDecimal getNewSubtotal() {
        return newSubtotal;
    }

    public void setNewSubtotal(BigDecimal newSubtotal) {
        this.newSubtotal = newSubtotal;
    }

    public BigDecimal getAmountRemoved() {
        return amountRemoved;
    }

    public void setAmountRemoved(BigDecimal amountRemoved) {
        this.amountRemoved = amountRemoved;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
}
