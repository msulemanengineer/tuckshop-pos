package com.tuckshop.pos.dto;

public class VoidSaleRequest {
    private String reason;
    private String ownerPin;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOwnerPin() {
        return ownerPin;
    }

    public void setOwnerPin(String ownerPin) {
        this.ownerPin = ownerPin;
    }
}
