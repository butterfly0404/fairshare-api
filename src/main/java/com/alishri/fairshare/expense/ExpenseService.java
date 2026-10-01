package com.alishri.fairshare.expense;

import com.alishri.fairshare.common.NotFoundException;
import com.alishri.fairshare.group.ExpenseGroup;
import com.alishri.fairshare.group.GroupMembershipRepository;
import com.alishri.fairshare.group.GroupRepository;
import com.alishri.fairshare.user.User;
import com.alishri.fairshare.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMembershipRepository membershipRepository;
    private final SplitCalculator splitCalculator;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupRepository groupRepository,
                          UserRepository userRepository,
                          GroupMembershipRepository membershipRepository,
                          SplitCalculator splitCalculator) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.splitCalculator = splitCalculator;
    }

    /**
     * Records an expense and its splits in one transaction.
     *
     * <p>Everything is validated before anything is written: a half-recorded
     * expense would silently corrupt every balance in the group.
     */
    @Transactional
    public Expense record(Long groupId, ExpenseDtos.CreateExpenseRequest request) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group", groupId));

        User payer = loadMember(groupId, request.paidById());
        List<User> participants = request.participantIds().stream()
                .map(id -> loadMember(groupId, id))
                .toList();

        Map<User, BigDecimal> shares = calculateShares(request, participants);

        Expense expense = new Expense(
                group, payer, request.description(), request.amount(), request.splitType());
        shares.forEach(expense::addSplit);

        return expenseRepository.save(expense);
    }

    private Map<User, BigDecimal> calculateShares(ExpenseDtos.CreateExpenseRequest request,
                                                  List<User> participants) {
        return switch (request.splitType()) {
            case EQUAL -> splitCalculator.equal(request.amount(), participants);
            case EXACT -> splitCalculator.exact(request.amount(), byUser(request, participants));
            case PERCENTAGE -> splitCalculator.percentage(request.amount(), byUser(request, participants));
        };
    }

    /** Maps the request's userId→value entries onto the loaded participants. */
    private Map<User, BigDecimal> byUser(ExpenseDtos.CreateExpenseRequest request,
                                         List<User> participants) {
        if (request.shares() == null || request.shares().isEmpty()) {
            throw new IllegalArgumentException(
                    "shares are required for a " + request.splitType() + " split");
        }

        Map<Long, User> byId = participants.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<User, BigDecimal> result = new LinkedHashMap<>();
        request.shares().forEach((userId, value) -> {
            User user = byId.get(userId);
            if (user == null) {
                throw new IllegalArgumentException(
                        "User %d has a share but is not a participant".formatted(userId));
            }
            result.put(user, value);
        });
        return result;
    }

    private User loadMember(Long groupId, Long userId) {
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException(
                    "User %d is not a member of group %d".formatted(userId, groupId));
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public List<Expense> listForGroup(Long groupId) {
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }
}