package com.alishri.fairshare.settlement;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Stateless settlement endpoint: takes net balances directly rather than
 * reading them from a stored group. Useful for trying the algorithm without
 * creating any data, and it is what the README's quick example uses.
 *
 * <p>Errors are handled by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    private final DebtSimplifier debtSimplifier;

    // Constructor injection — no @Autowired needed on a single constructor.
    // Preferred over field injection: dependencies are explicit and the class is
    // testable without a Spring context.
    public SettlementController(DebtSimplifier debtSimplifier) {
        this.debtSimplifier = debtSimplifier;
    }

    @PostMapping("/simplify")
    public List<Transaction> simplify(@RequestBody Map<String, BigDecimal> netBalances) {
        return debtSimplifier.simplify(netBalances);
    }
}