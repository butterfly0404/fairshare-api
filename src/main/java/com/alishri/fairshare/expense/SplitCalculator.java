package com.alishri.fairshare.expense;

import com.alishri.fairshare.user.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an expense amount plus a participant list into each person's share.
 *
 * <p>Every strategy guarantees the shares sum exactly to the expense amount.
 * That is the whole difficulty: ₹100 split three ways is 33.33 each, which
 * sums to 99.99. The missing paisa has to land somewhere explicit rather than
 * silently vanishing.
 */
@Component
public class SplitCalculator {

    private static final int MONEY_SCALE = 2;

    /**
     * Divides evenly, giving any rounding remainder to the first participant.
     *
     * <p>Assigning the remainder deterministically rather than distributing it
     * randomly keeps the operation reproducible — the same inputs always give
     * the same splits, which matters for testing and for user trust.
     */
    public Map<User, BigDecimal> equal(BigDecimal amount, List<User> participants) {
        requireParticipants(participants);

        BigDecimal count = BigDecimal.valueOf(participants.size());
        BigDecimal baseShare = amount.divide(count, MONEY_SCALE, RoundingMode.DOWN);

        Map<User, BigDecimal> shares = new LinkedHashMap<>();
        for (User participant : participants) {
            shares.put(participant, baseShare);
        }

        // Whatever rounding DOWN left behind goes to the first participant.
        BigDecimal distributed = baseShare.multiply(count);
        BigDecimal remainder = amount.subtract(distributed);
        if (remainder.signum() != 0) {
            User first = participants.get(0);
            shares.put(first, shares.get(first).add(remainder));
        }

        return shares;
    }

    /** Caller supplies each share; we verify they add up to the expense. */
    public Map<User, BigDecimal> exact(BigDecimal amount, Map<User, BigDecimal> shares) {
        requireParticipants(List.copyOf(shares.keySet()));

        BigDecimal total = shares.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(amount) != 0) {
            throw new IllegalArgumentException(
                    "Shares total %s but the expense is %s".formatted(total, amount));
        }
        shares.values().forEach(this::requireNonNegative);

        return new LinkedHashMap<>(shares);
    }

    /**
     * Caller supplies percentages totalling 100; the remainder from rounding
     * each percentage goes to the first participant, as with equal splits.
     */
    public Map<User, BigDecimal> percentage(BigDecimal amount, Map<User, BigDecimal> percentages) {
        requireParticipants(List.copyOf(percentages.keySet()));

        BigDecimal totalPercent = percentages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercent.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException(
                    "Percentages must total 100 but totalled " + totalPercent);
        }

        Map<User, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal distributed = BigDecimal.ZERO;

        for (Map.Entry<User, BigDecimal> entry : percentages.entrySet()) {
            requireNonNegative(entry.getValue());
            BigDecimal share = amount
                    .multiply(entry.getValue())
                    .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.DOWN);
            shares.put(entry.getKey(), share);
            distributed = distributed.add(share);
        }

        BigDecimal remainder = amount.subtract(distributed);
        if (remainder.signum() != 0) {
            User first = percentages.keySet().iterator().next();
            shares.put(first, shares.get(first).add(remainder));
        }

        return shares;
    }

    private void requireParticipants(List<User> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("An expense needs at least one participant");
        }
    }

    private void requireNonNegative(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("A share cannot be negative: " + value);
        }
    }
}