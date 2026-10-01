package com.alishri.fairshare.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A set of people sharing expenses: a trip, a flat, a recurring team lunch.
 *
 * <p>Named {@code ExpenseGroup} rather than {@code Group} because {@code group}
 * is reserved in SQL (GROUP BY) and {@code java.lang} already has enough
 * generic names competing for attention in imports.
 *
 * <p>Deliberately holds no collection of members or expenses. A group could
 * accumulate thousands of expenses, and mapping them as a {@code @OneToMany}
 * invites loading all of them to answer a question about one. Membership and
 * expenses are queried through their own repositories instead.
 */
@Entity
@Table(name = "expense_group")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    protected ExpenseGroup() {
        // Required by JPA.
    }

    public ExpenseGroup(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExpenseGroup group)) return false;
        return id != null && id.equals(group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ExpenseGroup{id=%d, name='%s'}".formatted(id, name);
    }
}