package com.alishri.fairshare.settlement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtSimplifierTest {

    private final DebtSimplifier simplifier = new DebtSimplifier();

    private static BigDecimal rs(String amount) {
        return new BigDecimal(amount);
    }

    @Test
    @DisplayName("empty input produces an empty plan")
    void emptyInput() {
        assertThat(simplifier.simplify(Map.of())).isEmpty();
        assertThat(simplifier.simplify(null)).isEmpty();
    }

    @Test
    @DisplayName("a settled group needs no transactions")
    void everyoneAlreadySettled() {
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("0"),
                "Bob", rs("0"));

        assertThat(simplifier.simplify(balances)).isEmpty();
    }

    @Test
    @DisplayName("one debtor pays one creditor directly")
    void simplestCase() {
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("100"),
                "Bob", rs("-100"));

        assertThat(simplifier.simplify(balances))
                .containsExactly(new Transaction("Bob", "Alice", rs("100")));
    }

    @Test
    @DisplayName("never exceeds n-1 transactions for n participants")
    void respectsLowerBound() {
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("1200"),
                "Bob", rs("-450"),
                "Carol", rs("300"),
                "Dave", rs("-800"),
                "Eve", rs("-250"));

        List<Transaction> plan = simplifier.simplify(balances);

        assertThat(plan).hasSizeLessThanOrEqualTo(balances.size() - 1);
    }

    @Test
    @DisplayName("members with a zero balance are left out of the plan")
    void skipsSettledMembers() {
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("50"),
                "Bob", rs("-50"),
                "Carol", rs("0"));

        List<Transaction> plan = simplifier.simplify(balances);

        assertThat(plan).noneMatch(t -> t.from().equals("Carol") || t.to().equals("Carol"));
    }

    @Test
    @DisplayName("every participant's net position is fully settled by the plan")
    void conservesMoney() {
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("1200"),
                "Bob", rs("-450"),
                "Carol", rs("300"),
                "Dave", rs("-800"),
                "Eve", rs("-250"));

        List<Transaction> plan = simplifier.simplify(balances);

        balances.forEach((member, expected) -> {
            BigDecimal received = plan.stream()
                    .filter(t -> t.to().equals(member))
                    .map(Transaction::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paid = plan.stream()
                    .filter(t -> t.from().equals(member))
                    .map(Transaction::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(received.subtract(paid))
                    .as("net movement for %s", member)
                    .isEqualByComparingTo(expected);
        });
    }

    @Test
    @DisplayName("handles the uneven paise left over when a bill splits three ways")
    void handlesRoundingRemainder() {
        // Rs 100 split three ways cannot divide evenly: one person carries the extra paisa.
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("66.67"),
                "Bob", rs("-33.33"),
                "Carol", rs("-33.34"));

        List<Transaction> plan = simplifier.simplify(balances);

        BigDecimal totalMoved = plan.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalMoved).isEqualByComparingTo(rs("66.67"));
    }

    @Test
    @DisplayName("rejects balances that do not net to zero")
    void rejectsUnbalancedInput() {
        Map<String, BigDecimal> balances = Map.of(
                "Alice", rs("500"),
                "Bob", rs("-100"));

        assertThatThrownBy(() -> simplifier.simplify(balances))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum to zero");
    }
}