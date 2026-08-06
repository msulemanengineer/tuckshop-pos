package com.tuckshop.pos.controller;

import com.tuckshop.pos.dto.ApiError;
import com.tuckshop.pos.dto.PaymentRequest;
import com.tuckshop.pos.model.CreditTransaction;
import com.tuckshop.pos.model.Customer;
import com.tuckshop.pos.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerApiController {

    private final CustomerService customerService;

    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> all(@RequestParam(required = false) String q) {
        return customerService.search(q);
    }

    @GetMapping("/outstanding")
    public List<Customer> outstanding() {
        return customerService.outstanding();
    }

    @GetMapping("/outstanding-total")
    public BigDecimal outstandingTotal() {
        return customerService.totalOutstanding();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> byId(@PathVariable Long id) {
        return customerService.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("Customer not found")));
    }

    @GetMapping("/{id}/ledger")
    public List<CreditTransaction> ledger(@PathVariable Long id) {
        return customerService.ledger(id);
    }

    // Both roles can add a new regular customer on the spot - common when a new
    // customer at the pump asks to open a khata account.
    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        customer.setId(null);
        return customerService.createOrUpdate(customer);
    }

    // Editing an existing customer's credit limit is an owner decision - a cashier
    // raising their own credit limit would defeat the point of having one.
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Customer customer) {
        return customerService.findById(id).<ResponseEntity<?>>map(existing -> {
            customer.setId(id);
            return ResponseEntity.ok(customerService.createOrUpdate(customer));
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("Customer not found")));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Recording a payment (customer paying back some of what they owe) is fine for
    // either role - this is a routine daily task, not a policy change.
    @PostMapping("/{id}/payments")
    public CreditTransaction recordPayment(@PathVariable Long id, @RequestBody PaymentRequest request,
                                            Authentication auth) {
        return customerService.recordPayment(id, request.getAmount(), request.getNote(), auth.getName());
    }
}
