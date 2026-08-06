package com.tuckshop.pos.service;

import com.tuckshop.pos.dto.CartItemDTO;
import com.tuckshop.pos.dto.CheckoutRequest;
import com.tuckshop.pos.model.Customer;
import com.tuckshop.pos.model.Product;
import com.tuckshop.pos.model.Sale;
import com.tuckshop.pos.model.SaleItem;
import com.tuckshop.pos.model.SaleEditLog;
import com.tuckshop.pos.repository.CustomerRepository;
import com.tuckshop.pos.repository.ProductRepository;
import com.tuckshop.pos.repository.SaleEditLogRepository;
import com.tuckshop.pos.repository.SaleRepository;
import com.tuckshop.pos.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerService customerService;
    private final ActivityLogService activityLogService;
    private final LicenseService licenseService;
    private final SaleEditLogRepository saleEditLogRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
                        CustomerRepository customerRepository, UserRepository userRepository,
                        PasswordEncoder passwordEncoder, CustomerService customerService,
                        ActivityLogService activityLogService, LicenseService licenseService,
                        SaleEditLogRepository saleEditLogRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerService = customerService;
        this.activityLogService = activityLogService;
        this.licenseService = licenseService;
        this.saleEditLogRepository = saleEditLogRepository;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    @Transactional
    public Sale checkout(CheckoutRequest request) {
        if (!licenseService.isValid()) {
            throw new IllegalStateException(licenseService.blockedMessage());
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        String method = request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()
                ? "CASH" : request.getPaymentMethod().toUpperCase();

        Sale sale = new Sale();
        sale.setPaymentMethod(method);
        sale.setCashierUsername(currentUsername());

        Customer customer = null;
        if ("KHATA".equals(method)) {
            if (request.getCustomerId() == null) {
                throw new IllegalArgumentException("Select a customer for a credit (khata) sale.");
            }
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            sale.setCustomer(customer);
            sale.setCustomerNameSnapshot(customer.getName());
        }

        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDTO cartItem : request.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: id " + cartItem.getProductId()));

            int qty = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            if (qty <= 0) {
                throw new IllegalArgumentException("Invalid quantity for " + product.getName());
            }
            if (product.getQuantity() < qty) {
                throw new IllegalStateException("Not enough stock for " + product.getName()
                        + " (only " + product.getQuantity() + " left)");
            }

            product.setQuantity(product.getQuantity() - qty);
            productRepository.save(product);

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setQuantity(qty);
            item.setUnitPrice(product.getSellingPrice());
            item.setUnitCost(product.getCostPrice());
            item.setSubtotal(product.getSellingPrice().multiply(BigDecimal.valueOf(qty)));

            sale.addItem(item);
            total = total.add(item.getSubtotal());
        }

        sale.setTotalAmount(total);
        Sale saved = saleRepository.save(sale);

        if (customer != null) {
            customerService.chargeForSale(customer, total, saved.getId(), currentUsername());
        }

        return saved;
    }

    public List<Sale> recentSales() {
        return saleRepository.findTop10ByOrderBySaleDateDesc();
    }

    public List<Sale> findBetween(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(start, end);
    }

    public List<Sale> findAll() {
        return saleRepository.findAll().stream()
                .sorted((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()))
                .toList();
    }

    public Sale findById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found"));
    }

    /**
     * Voids a completed sale and restocks its items. Requires the owner's PIN, so a cashier
     * can call the owner (who may not be on-site) and void a mistaken sale with approval,
     * without needing the owner to physically log in.
     */
    /** Shared gate for any action that mutates a completed sale - void or edit. */
    private void validateOwnerPin(String ownerPin) {
        boolean pinValid = userRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(u -> "OWNER".equals(u.getRole()) && u.isActive() && u.getPinHash() != null)
                .anyMatch(u -> passwordEncoder.matches(ownerPin, u.getPinHash()));

        if (!pinValid) {
            throw new IllegalArgumentException("Incorrect owner PIN.");
        }
    }

    @Transactional
    public Sale voidSale(Long saleId, String reason, String ownerPin) {
        validateOwnerPin(ownerPin);

        Sale sale = findById(saleId);
        if ("VOIDED".equals(sale.getStatus())) {
            throw new IllegalStateException("This sale is already voided.");
        }

        // restock every item from this sale
        for (SaleItem item : sale.getItems()) {
            if (item.getProduct() != null) {
                Product p = item.getProduct();
                p.setQuantity(p.getQuantity() + item.getQuantity());
                productRepository.save(p);
            }
        }

        // reverse a khata charge if this was a credit sale
        if (sale.getCustomer() != null) {
            customerService.recordPayment(sale.getCustomer().getId(), sale.getTotalAmount(),
                    "Reversal for voided sale #" + sale.getId(), currentUsername());
        }

        sale.setStatus("VOIDED");
        sale.setVoidedBy(currentUsername());
        sale.setVoidReason(reason);

        activityLogService.log("SALE_VOIDED", "Sale #" + saleId + " voided by " + currentUsername()
                + " - reason: " + reason);

        return saleRepository.save(sale);
    }

    /**
     * Corrects the quantity on one line of a completed sale (e.g. customer decided they
     * wanted 3, not the 4 already rung up) - never increases a quantity, only reduces it,
     * since "more" is just a new sale. Requires the owner's PIN for the same reason voids
     * do: reducing an already-paid sale's total is exactly how a dishonest cashier would
     * pocket the difference, so it needs the same approval as cancelling one outright.
     * Every edit is written to SaleEditLog for the owner's review, on top of the general
     * activity log.
     */
    @Transactional
    public Sale editSaleItemQuantity(Long saleId, Long saleItemId, int newQuantity, String reason, String ownerPin) {
        validateOwnerPin(ownerPin);

        Sale sale = findById(saleId);
        if (!"COMPLETED".equals(sale.getStatus())) {
            throw new IllegalStateException("Only completed sales can be edited.");
        }

        SaleItem item = sale.getItems().stream()
                .filter(i -> i.getId().equals(saleItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That item isn't part of this sale."));

        int oldQuantity = item.getQuantity();
        if (newQuantity < 0 || newQuantity >= oldQuantity) {
            throw new IllegalArgumentException("New quantity must be less than the current quantity (" + oldQuantity + ").");
        }

        BigDecimal oldSubtotal = item.getSubtotal();
        int returnedQty = oldQuantity - newQuantity;

        if (item.getProduct() != null) {
            Product p = item.getProduct();
            p.setQuantity(p.getQuantity() + returnedQty);
            productRepository.save(p);
        }

        BigDecimal newSubtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity));
        BigDecimal amountRemoved = oldSubtotal.subtract(newSubtotal);

        if (newQuantity == 0) {
            sale.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
            item.setSubtotal(newSubtotal);
        }

        sale.setTotalAmount(sale.getTotalAmount().subtract(amountRemoved));

        // reduce the khata charge to match, if this was a credit sale
        if (sale.getCustomer() != null && amountRemoved.compareTo(BigDecimal.ZERO) > 0) {
            customerService.recordPayment(sale.getCustomer().getId(), amountRemoved,
                    "Correction for sale #" + sale.getId() + " (" + item.getProductName() + ")", currentUsername());
        }

        // nothing left on the sale - treat it the same as a void rather than leaving a
        // zero-item "completed" sale sitting in history
        if (sale.getItems().isEmpty()) {
            sale.setStatus("VOIDED");
            sale.setVoidedBy(currentUsername());
            sale.setVoidReason("All items removed via edit: " + reason);
        }

        Sale saved = saleRepository.save(sale);

        SaleEditLog editLog = new SaleEditLog();
        editLog.setSaleId(saleId);
        editLog.setProductName(item.getProductName());
        editLog.setCashierUsername(currentUsername());
        editLog.setOldQuantity(oldQuantity);
        editLog.setNewQuantity(newQuantity);
        editLog.setOldSubtotal(oldSubtotal);
        editLog.setNewSubtotal(newSubtotal);
        editLog.setAmountRemoved(amountRemoved);
        editLog.setReason(reason);
        saleEditLogRepository.save(editLog);

        activityLogService.log("SALE_ITEM_EDITED", "Sale #" + saleId + " - " + item.getProductName()
                + " qty " + oldQuantity + " -> " + newQuantity + " (Rs " + amountRemoved + " removed) by "
                + currentUsername() + " - reason: " + reason);

        return saved;
    }
}
