package com.example.militarylogisticsmgmt.repository;

import com.example.militarylogisticsmgmt.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>This interface extends {@link JpaRepository} to provide standard CRUD operations and allows
 * the definition of additional query methods for User data access. It is annotated with
 * {@link Repository} to indicate its role in Spring Data.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Retrieves a User entity by its username.
   *
   * <p>This method returns an {@link Optional} to indicate whether a User was found with the
   * specified username.
   *
   * @param username the username of the User to retrieve.
   * @return an {@link Optional} containing the User if found, or empty if not.
   */
  Optional<User> findByUsername(String username);
}
