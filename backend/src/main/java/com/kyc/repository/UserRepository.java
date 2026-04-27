package com.kyc.repository;

import com.kyc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Simple repository for User entity.
 * Spring Data JPA auto-implements all the SQL queries.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
