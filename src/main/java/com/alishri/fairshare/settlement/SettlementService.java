package com.alishri.fairshare.settlement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Connects stored expense data to the settlement algorithm: aggregate the
 * group's history into net balances, then reduce those to the minimum
 * payment set.
 */
@Service
public class SettlementService {

    private final BalanceCalculator balanceCalculator;
    private final DebtSimplifier debtSimplifier;

    public SettlementService(BalanceCalculator balanceCalculator,
                             DebtSimplifier debtSimplifier) {
        this.balanceCalculator = balanceCalculator;
        this.debtSimplifier = debtSimplifier;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> balancesFor(Long groupId) {
        return balanceCalculator.netBalancesByName(groupId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> settlementPlanFor(Long groupId) {
        return debtSimplifier.simplify(balanceCalculator.netBalancesByName(groupId));
    }
}