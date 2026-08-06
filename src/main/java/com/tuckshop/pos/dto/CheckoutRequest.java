package com.tuckshop.pos.dto;

import java.util.List;

public class CheckoutRequest {
    private List<CartItemDTO> items;
    private String paymentMethod;
    private Long customerId; // only used when paymentMethod = KHATA

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
