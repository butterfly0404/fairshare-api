package com.alishri.fairshare.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data generates the implementation at runtime from these method
 * signatures — there is no class to write.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used to reject duplicate registrations before hitting the unique constraint. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Fetches every member of a group in one query, for balance calculation. */
    List<User> findByIdIn(Collection<Long> ids);
}