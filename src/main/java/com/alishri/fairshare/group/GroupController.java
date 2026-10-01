package com.alishri.fairshare.group;

import com.alishri.fairshare.expense.ExpenseDtos;
import com.alishri.fairshare.expense.ExpenseService;
import com.alishri.fairshare.settlement.SettlementService;
import com.alishri.fairshare.settlement.Transaction;
import com.alishri.fairshare.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;

    public GroupController(GroupService groupService,
                           ExpenseService expenseService,
                           SettlementService settlementService) {
        this.groupService = groupService;
        this.expenseService = expenseService;
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDtos.GroupResponse create(
            @Valid @RequestBody GroupDtos.CreateGroupRequest request) {
        return GroupDtos.GroupResponse.from(groupService.create(request.name()));
    }

    @GetMapping("/{groupId}")
    public GroupDtos.GroupResponse get(@PathVariable Long groupId) {
        return GroupDtos.GroupResponse.from(groupService.findById(groupId));
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDtos.MemberResponse addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupDtos.AddMemberRequest request) {
        User user = groupService.addMember(groupId, request.name(), request.email());
        return new GroupDtos.MemberResponse(user.getId(), user.getName(), user.getEmail());
    }

    @GetMapping("/{groupId}/members")
    public List<GroupDtos.MemberResponse> members(@PathVariable Long groupId) {
        return groupService.membersOf(groupId).stream()
                .map(u -> new GroupDtos.MemberResponse(u.getId(), u.getName(), u.getEmail()))
                .toList();
    }

    @PostMapping("/{groupId}/expenses")
    public ResponseEntity<ExpenseDtos.ExpenseResponse> addExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody ExpenseDtos.CreateExpenseRequest request) {

        var expense = expenseService.record(groupId, request);

        var splits = expense.getSplits().stream()
                .map(s -> new ExpenseDtos.SplitResponse(
                        s.getUser().getId(), s.getUser().getName(), s.getShare()))
                .toList();

        var body = new ExpenseDtos.ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getName(),
                expense.getSplitType(),
                splits);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Raw net position per member: positive is owed, negative owes. */
    @GetMapping("/{groupId}/balances")
    public Map<String, BigDecimal> balances(@PathVariable Long groupId) {
        groupService.findById(groupId);
        return settlementService.balancesFor(groupId);
    }

    /** The minimum set of payments that settles the group. */
    @GetMapping("/{groupId}/settlements")
    public List<Transaction> settlements(@PathVariable Long groupId) {
        groupService.findById(groupId);
        return settlementService.settlementPlanFor(groupId);
    }
}