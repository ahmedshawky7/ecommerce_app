package com.example.ecommerce.repository;

import com.example.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // ============ Admin User Search ============

    @Query("SELECT u FROM User u " +
            "WHERE (:keyword IS NULL OR " +
            "  LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "  LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "ORDER BY u.createdAt DESC")
    org.springframework.data.domain.Page<User> adminSearchUsers(
            @Param("keyword") String keyword,
            @Param("role") com.example.ecommerce.entity.Role role,
            org.springframework.data.domain.Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}