package com.alishri.fairshare.settlement;

import com.alishri.fairshare.expense.ExpenseRepository;
import com.alishri.fairshare.expense.ExpenseSplitRepository;
import com.alishri.fairshare.expense.UserAmount;
import com.alishri.fairshare.group.GroupMembershipRepository;
import com.alishri.fairshare.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reduces a group's expense history to one net balance per member.
 *
 * <p>net = (total this person paid out) − (total this person owes)
 *
 * <p>Positive means the group owes them; negative means they owe the group.
 * By construction the balances sum to zero, which is what
 * {@link DebtSimplifier} requires.
 *
 * <p>Both totals come from SQL aggregates — two queries returning one row per
 * member, regardless of whether the group has ten expenses or ten thousand.
 */
@Service
public class BalanceCalculator {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupMembershipRepository membershipRepository;

    public BalanceCalculator(ExpenseRepository expenseRepository,
                             ExpenseSplitRepository splitRepository,
                             GroupMembershipRepository membershipRepository) {
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.membershipRepository = membershipRepository;
    }

    /** Net balance per member, keyed by display name, including members at zero. */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> netBalancesByName(Long groupId) {
        List<User> members = membershipRepository.findMembersOfGroup(groupId);

        Map<Long, BigDecimal> paid = toMap(expenseRepository.sumPaidPerUser(groupId));
        Map<Long, BigDecimal> owed = toMap(splitRepository.sumOwedPerUser(groupId));

        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        for (User member : members) {
            BigDecimal net = paid.getOrDefault(member.getId(), BigDecimal.ZERO)
                    .subtract(owed.getOrDefault(member.getId(), BigDecimal.ZERO));
            balances.put(member.getName(), net);
        }
        return balances;
    }

    private Map<Long, BigDecimal> toMap(List<UserAmount> rows) {
        return rows.stream().collect(Collectors.toMap(
                UserAmount::getUserId,
                UserAmount::getTotal,
                (a, b) -> a,
                LinkedHashMap::new));
    }
}