package com.alishri.fairshare.settlement;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Reduces a web of pairwise debts to the minimum number of settling transactions.
 *
 * <p>Given each member's <em>net</em> balance (positive = owed money, negative = owes money),
 * the original graph of who-paid-for-whom is discarded entirely. Only the net position matters:
 * if Alice is owed 500 in total, it is irrelevant whether that came from one expense or twenty.
 *
 * <p><strong>Algorithm.</strong> Greedy max-matching using two heaps. Repeatedly take the largest
 * creditor and the largest debtor and settle the smaller of the two amounts between them. That
 * payment zeroes out at least one of the pair, so each iteration permanently removes at least one
 * participant. With {@code n} participants this terminates in at most {@code n - 1} transactions,
 * which is the theoretical lower bound whenever no proper subset sums to zero.
 *
 * <p><strong>Complexity.</strong> O(n log n) — each participant is pushed and popped O(1) times
 * amortised, with log n heap operations.
 *
 * <p><strong>Note on optimality.</strong> Finding the true minimum in every case requires
 * detecting all zero-sum subsets, which is NP-hard (subset-sum). This greedy approach gives the
 * optimal n-1 bound in the general case and is what production splitting apps use.
 */
@Component
public class DebtSimplifier {

    /** A participant with a remaining unsettled amount. */
    private record Node(String user, BigDecimal amount) {
    }

    private static final Comparator<Node> LARGEST_FIRST =
            Comparator.comparing(Node::amount).reversed();

    public List<Transaction> simplify(Map<String, BigDecimal> netBalances) {
        if (netBalances == null || netBalances.isEmpty()) {
            return List.of();
        }
        validateSumsToZero(netBalances);

        PriorityQueue<Node> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Node> debtors = new PriorityQueue<>(LARGEST_FIRST);

        netBalances.forEach((user, amount) -> {
            int sign = amount.signum();
            if (sign > 0) {
                creditors.add(new Node(user, amount));
            } else if (sign < 0) {
                debtors.add(new Node(user, amount.negate()));
            }
            // sign == 0: already settled, skip
        });

        List<Transaction> plan = new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Node creditor = creditors.poll();
            Node debtor = debtors.poll();

            BigDecimal settled = creditor.amount().min(debtor.amount());
            plan.add(new Transaction(debtor.user(), creditor.user(), settled));

            BigDecimal creditorLeft = creditor.amount().subtract(settled);
            BigDecimal debtorLeft = debtor.amount().subtract(settled);

            if (creditorLeft.signum() > 0) {
                creditors.add(new Node(creditor.user(), creditorLeft));
            }
            if (debtorLeft.signum() > 0) {
                debtors.add(new Node(debtor.user(), debtorLeft));
            }
        }

        return plan;
    }

    /**
     * A valid set of balances must net to zero — every rupee owed is a rupee owed to someone.
     * A non-zero sum means the caller's data is wrong, so we fail loudly rather than
     * silently producing a settlement plan that leaves money unaccounted for.
     */
    private void validateSumsToZero(Map<String, BigDecimal> netBalances) {
        BigDecimal total = netBalances.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) {
            throw new IllegalArgumentException(
                    "Net balances must sum to zero but summed to " + total);
        }
    }
}