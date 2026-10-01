package com.alishri.fairshare.expense;

import com.alishri.fairshare.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One participant's share of one expense.
 *
 * <p>The shares of an expense must sum to the expense amount. That invariant
 * is enforced in the service layer rather than in SQL, where expressing a
 * cross-row sum constraint cheaply is not possible.
 */
@Entity
@Table(
        name = "expense_split",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_expense_participant",
                columnNames = {"expense_id", "user_id"}))
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal share;

    protected ExpenseSplit() {
        // Required by JPA.
    }

    ExpenseSplit(Expense expense, User user, BigDecimal share) {
        // Package-private: splits are only created via Expense.addSplit().
        this.expense = expense;
        this.user = user;
        this.share = share;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getShare() {
        return share;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExpenseSplit split)) return false;
        return id != null && id.equals(split.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}