package com.alishri.fairshare.group;

import com.alishri.fairshare.common.NotFoundException;
import com.alishri.fairshare.user.User;
import com.alishri.fairshare.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMembershipRepository membershipRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ExpenseGroup create(String name) {
        return groupRepository.save(new ExpenseGroup(name));
    }

    @Transactional(readOnly = true)
    public ExpenseGroup findById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group", groupId));
    }

    /**
     * Adds a member, reusing an existing user if that email is already known.
     *
     * <p>Email is the natural identifier for a person across groups — the same
     * individual in a flat group and a trip group should be one user, not two.
     */
    @Transactional
    public User addMember(Long groupId, String name, String email) {
        ExpenseGroup group = findById(groupId);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(name, email)));

        if (membershipRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new IllegalArgumentException(
                    "%s is already a member of this group".formatted(email));
        }

        membershipRepository.save(new GroupMembership(group, user));
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> membersOf(Long groupId) {
        findById(groupId);
        return membershipRepository.findMembersOfGroup(groupId);
    }
}