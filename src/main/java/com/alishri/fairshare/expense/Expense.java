package com.alishri.fairshare.expense;

import com.alishri.fairshare.group.ExpenseGroup;
import com.alishri.fairshare.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One expense: a single person paid, to be shared among several.
 *
 * <p>This is the aggregate root for its splits. Unlike the group-to-expenses
 * relationship, the splits collection is bounded by the size of the group, is
 * always needed whenever the expense is, and has no meaning on its own — so a
 * cascading {@code @OneToMany} is the right call here and a performance hazard
 * there. The rule is "avoid unbounded collections", not "avoid collections".
 */
@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_id", nullable = false)
    private User paidBy;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "expense",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ExpenseSplit> splits = new ArrayList<>();

    protected Expense() {
        // Required by JPA.
    }

    public Expense(ExpenseGroup group, User paidBy, String description,
                   BigDecimal amount, SplitType splitType) {
        this.group = group;
        this.paidBy = paidBy;
        this.description = description;
        this.amount = amount;
        this.splitType = splitType;
    }

    /**
     * Adds a participant's share, keeping both sides of the relationship in
     * step. Setting only one side is a classic JPA bug: the in-memory object
     * graph and what gets written to the database disagree.
     */
    public void addSplit(User participant, BigDecimal share) {
        splits.add(new ExpenseSplit(this, participant, share));
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Unmodifiable: splits are added through {@link #addSplit}, never by mutating this list. */
    public List<ExpenseSplit> getSplits() {
        return Collections.unmodifiableList(splits);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Expense expense)) return false;
        return id != null && id.equals(expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}