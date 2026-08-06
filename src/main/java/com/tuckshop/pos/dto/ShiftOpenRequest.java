package com.tuckshop.pos.dto;

import java.math.BigDecimal;

public class ShiftOpenRequest {
    private BigDecimal openingCash;

    public BigDecimal getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(BigDecimal openingCash) {
        this.openingCash = openingCash;
    }
}
