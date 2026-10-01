package com.alishri.fairshare.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    /**
     * Total each member owes across a group's expenses.
     *
     * <p>Aggregating in the database rather than loading every split row and
     * summing in Java: two rows per member come back instead of potentially
     * thousands of entities.
     */
    @Query("""
            SELECT s.user.id AS userId, SUM(s.share) AS total
            FROM ExpenseSplit s
            WHERE s.expense.group.id = :groupId
            GROUP BY s.user.id
            """)
    List<UserAmount> sumOwedPerUser(@Param("groupId") Long groupId);
}