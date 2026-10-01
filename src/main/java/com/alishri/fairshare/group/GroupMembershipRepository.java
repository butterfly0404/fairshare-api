package com.alishri.fairshare.group;

import com.alishri.fairshare.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * Loads a group's members in a single query, oldest member first.
     *
     * <p>Selects the user directly rather than the membership. A JOIN FETCH
     * would be wrong here — it eagerly loads an association onto a selected
     * owner entity, and the owner is not in the select list. It is not needed
     * either: the user is the result, so no lazy association is left behind to
     * trigger an N+1.
     */
    @Query("""
            SELECT m.user
            FROM GroupMembership m
            WHERE m.group.id = :groupId
            ORDER BY m.joinedAt
            """)
    List<User> findMembersOfGroup(@Param("groupId") Long groupId);

    long countByGroupId(Long groupId);
}