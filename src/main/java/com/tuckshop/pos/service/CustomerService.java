package com.tuckshop.pos.service;

import com.tuckshop.pos.model.CreditTransaction;
import com.tuckshop.pos.model.Customer;
import com.tuckshop.pos.repository.CreditTransactionRepository;
import com.tuckshop.pos.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final ActivityLogService activityLogService;

    public CustomerService(CustomerRepository customerRepository,
                            CreditTransactionRepository creditTransactionRepository,
                            ActivityLogService activityLogService) {
        this.customerRepository = customerRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.activityLogService = activityLogService;
    }

    public List<Customer> search(String term) {
        if (term == null || term.isBlank()) {
            return customerRepository.findAll();
        }
        return customerRepository.findByNameContainingIgnoreCaseOrPhoneContaining(term, term);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> outstanding() {
        return customerRepository.findAllWithOutstandingBalance();
    }

    public BigDecimal totalOutstanding() {
        return customerRepository.sumAllOutstandingBalances();
    }

    public Customer createOrUpdate(Customer customer) {
        return customerRepository.save(customer);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
        activityLogService.log("CUSTOMER_DELETED", "Deleted customer id " + id);
    }

    public List<CreditTransaction> ledger(Long customerId) {
        return creditTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /** Charges a customer's account for a khata (credit) sale. Called from checkout. */
    @Transactional
    public void chargeForSale(Customer customer, BigDecimal amount, Long saleId, String recordedBy) {
        customer.setCurrentBalance(customer.getCurrentBalance().add(amount));
        customerRepository.save(customer);

        CreditTransaction tx = new CreditTransaction();
        tx.setCustomer(customer);
        tx.setType("CHARGE");
        tx.setAmount(amount);
        tx.setSaleId(saleId);
        tx.setNote("Credit sale");
        tx.setRecordedBy(recordedBy);
        creditTransactionRepository.save(tx);
    }

    /** Records a payment received from a customer against their outstanding balance. */
    @Transactional
    public CreditTransaction recordPayment(Long customerId, BigDecimal amount, String note, String recordedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        customer.setCurrentBalance(customer.getCurrentBalance().subtract(amount));
        customerRepository.save(customer);

        CreditTransaction tx = new CreditTransaction();
        tx.setCustomer(customer);
        tx.setType("PAYMENT");
        tx.setAmount(amount);
        tx.setNote(note);
        tx.setRecordedBy(recordedBy);
        return creditTransactionRepository.save(tx);
    }
}
