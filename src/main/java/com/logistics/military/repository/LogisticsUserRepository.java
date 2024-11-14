package com.logistics.military.repository;

import com.logistics.military.model.LogisticsUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link LogisticsUser} entities.
 *
 * <p>This interface extends {@link JpaRepository} to provide standard CRUD operations and allows
 * the definition of additional query methods for User data access. It is annotated with
 * {@link Repository} to indicate its role in Spring Data.
 */
@Repository
public interface LogisticsUserRepository extends JpaRepository<LogisticsUser, Long> {

  /**
   * Retrieves a {@link LogisticsUser} entity by its username.
   *
   * <p>This method returns an {@link Optional} to indicate whether a User was found with the
   * specified username.
   *
   * @param username the username of the {@link LogisticsUser} to retrieve.
   * @return an {@link Optional} containing the {@link LogisticsUser} if found, or empty if not.
   */
  Optional<LogisticsUser> findByUsername(String username);
}
