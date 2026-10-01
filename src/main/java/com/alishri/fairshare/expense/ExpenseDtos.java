package com.alishri.fairshare.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    /**
     * @param participantIds who shares this expense
     * @param shares         required for EXACT and PERCENTAGE splits, ignored for EQUAL
     */
    public record CreateExpenseRequest(
            @NotBlank(message = "Description is required")
            @Size(max = 255)
            String description,

            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            BigDecimal amount,

            @NotNull(message = "paidById is required")
            Long paidById,

            @NotNull(message = "splitType is required")
            SplitType splitType,

            @NotEmpty(message = "At least one participant is required")
            List<Long> participantIds,

            Map<Long, BigDecimal> shares) {
    }

    public record ExpenseResponse(
            Long id,
            String description,
            BigDecimal amount,
            String paidBy,
            SplitType splitType,
            List<SplitResponse> splits) {
    }

    public record SplitResponse(Long userId, String name, BigDecimal share) {
    }
}