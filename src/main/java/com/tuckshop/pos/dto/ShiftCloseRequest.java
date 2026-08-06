package com.tuckshop.pos.dto;

import java.math.BigDecimal;

public class ShiftCloseRequest {
    private BigDecimal actualClosingCash;

    public BigDecimal getActualClosingCash() {
        return actualClosingCash;
    }

    public void setActualClosingCash(BigDecimal actualClosingCash) {
        this.actualClosingCash = actualClosingCash;
    }
}
