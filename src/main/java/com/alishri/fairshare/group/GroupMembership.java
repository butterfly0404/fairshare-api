package com.alishri.fairshare.group;

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

import java.time.Instant;
import java.util.Objects;

/**
 * Links a user to a group. Modelled as an entity in its own right rather than
 * a plain {@code @ManyToMany} join table, because membership carries its own
 * data ({@code joinedAt}) and will carry more later — a role, or a left-at
 * date for people who leave a group mid-trip.
 *
 * <p>A {@code @ManyToMany} would hide the join table and make that impossible
 * to extend without a migration of the mapping itself.
 */
@Entity
@Table(
        name = "group_membership",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_group_member",
                columnNames = {"group_id", "user_id"}))
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false, insertable = false)
    private Instant joinedAt;

    protected GroupMembership() {
        // Required by JPA.
    }

    public GroupMembership(ExpenseGroup group, User user) {
        this.group = group;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GroupMembership membership)) return false;
        return id != null && id.equals(membership.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}