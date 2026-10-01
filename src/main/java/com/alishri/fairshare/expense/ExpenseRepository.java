package com.alishri.fairshare.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    /**
     * Total each member has paid out in a group. Half of the balance equation;
     * the other half is what they owe, from ExpenseSplitRepository.
     */
    @Query("""
            SELECT e.paidBy.id AS userId, SUM(e.amount) AS total
            FROM Expense e
            WHERE e.group.id = :groupId
            GROUP BY e.paidBy.id
            """)
    List<UserAmount> sumPaidPerUser(@Param("groupId") Long groupId);
}