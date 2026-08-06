package com.tuckshop.pos.dto;

public class EditSaleItemRequest {
    private Integer newQuantity;
    private String reason;
    private String ownerPin;

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

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
