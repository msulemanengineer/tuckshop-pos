package com.tuckshop.pos.service;

import com.tuckshop.pos.model.CreditTransaction;
import com.tuckshop.pos.model.Sale;
import com.tuckshop.pos.model.Shift;
import com.tuckshop.pos.repository.CreditTransactionRepository;
import com.tuckshop.pos.repository.SaleRepository;
import com.tuckshop.pos.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final SaleRepository saleRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    public ShiftService(ShiftRepository shiftRepository, SaleRepository saleRepository,
                         CreditTransactionRepository creditTransactionRepository) {
        this.shiftRepository = shiftRepository;
        this.saleRepository = saleRepository;
        this.creditTransactionRepository = creditTransactionRepository;
    }

    public Optional<Shift> currentOpenShift(String username) {
        return shiftRepository.findFirstByCashierUsernameAndStatusOrderByOpenedAtDesc(username, "OPEN");
    }

    @Transactional
    public Shift openShift(String username, BigDecimal openingCash) {
        if (currentOpenShift(username).isPresent()) {
            throw new IllegalStateException("You already have an open shift. Close it before starting a new one.");
        }
        Shift shift = new Shift();
        shift.setCashierUsername(username);
        shift.setOpeningCash(openingCash);
        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift closeShift(String username, BigDecimal actualClosingCash) {
        Shift shift = currentOpenShift(username)
                .orElseThrow(() -> new IllegalStateException("No open shift to close."));

        LocalDateTime now = LocalDateTime.now();

        List<Sale> salesDuringShift = saleRepository
                .findBySaleDateBetweenOrderBySaleDateDesc(shift.getOpenedAt(), now)
                .stream()
                .filter(s -> username.equals(s.getCashierUsername()))
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .filter(s -> "CASH".equals(s.getPaymentMethod()))
                .toList();

        BigDecimal cashSales = salesDuringShift.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Khata payments this cashier collected during the shift are real cash too -
        // without this, the drawer would look "over" and flag a false discrepancy
        BigDecimal khataPayments = creditTransactionRepository
                .findPaymentsByUserBetween(username, shift.getOpenedAt(), now)
                .stream()
                .map(CreditTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expected = shift.getOpeningCash().add(cashSales).add(khataPayments);

        shift.setExpectedClosingCash(expected);
        shift.setActualClosingCash(actualClosingCash);
        shift.setDifference(actualClosingCash.subtract(expected));
        shift.setClosedAt(now);
        shift.setStatus("CLOSED");

        return shiftRepository.save(shift);
    }

    public List<Shift> history() {
        return shiftRepository.findAllByOrderByOpenedAtDesc();
    }
}
