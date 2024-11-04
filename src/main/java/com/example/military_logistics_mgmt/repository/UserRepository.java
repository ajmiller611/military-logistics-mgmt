package com.example.military_logistics_mgmt.repository;

import com.example.military_logistics_mgmt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 *
 * This interface extends {@link JpaRepository} to provide standard CRUD operations
 * and allows for the definition of additional query methods for User data access.
 * It is annotated with {@link Repository} to indicate its role in Spring Data.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Retrieves a User entity by its username.
     *
     * This method returns an {@link Optional} to indicate whether a User was found
     * with the specified username.
     *
     * @param username the username of the User to retrieve.
     * @return an {@link Optional} containing the User if found, or empty if not.
     */
    Optional<User> findByUsername(String username);
}
