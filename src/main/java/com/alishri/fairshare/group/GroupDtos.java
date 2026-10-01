package com.alishri.fairshare.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request and response shapes for the group endpoints.
 *
 * <p>Entities are never exposed directly over HTTP. A DTO lets the API contract
 * change independently of the database, keeps lazy associations from being
 * serialised by accident, and stops a client from setting fields it has no
 * business setting.
 */
public final class GroupDtos {

    private GroupDtos() {
    }

    public record CreateGroupRequest(
            @NotBlank(message = "Group name is required")
            @Size(max = 150, message = "Group name must be at most 150 characters")
            String name) {
    }

    public record AddMemberRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100)
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            @Size(max = 255)
            String email) {
    }

    public record GroupResponse(Long id, String name) {
        static GroupResponse from(ExpenseGroup group) {
            return new GroupResponse(group.getId(), group.getName());
        }
    }

    public record MemberResponse(Long id, String name, String email) {
    }
}