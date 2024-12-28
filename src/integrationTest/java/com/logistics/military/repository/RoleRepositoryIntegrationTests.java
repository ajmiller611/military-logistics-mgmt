package com.logistics.military.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.military.model.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the {@link RoleRepository} class.
 *
 * <p>This test class validates the integration between the {@code RoleRepository} and
 * the underlying database. It ensures that repository methods operate as expected, performing
 * accurate queries and handling edge cases such as empty results.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Query Accuracy:</strong> Ensures the {@code findByAuthority} method retrieves
 *     the correct {@link Role} when it exists in the database.
 *   </li>
 *   <li>
 *     <strong>Edge Case Handling:</strong> Verifies that the repository correctly returns
 *     an empty {@code Optional} when the specified role authority does not exist in the database.
 *   </li>
 * </ul>
 */
@ActiveProfiles("integration")
@DataJpaTest
class RoleRepositoryIntegrationTests {

  @Autowired private RoleRepository roleRepository;

  /** Verifies an existing role is returned when its authority matches the argument string. */
  @Test
  void givenRoleExistWhenFindByAuthorityThenReturnRole() {
    Role adminRole = roleRepository.save(new Role("ADMIN"));

    Optional<Role> fetchedRole = roleRepository.findByAuthority("ADMIN");

    assertNotNull(fetchedRole);
    assertTrue(fetchedRole.isPresent());
    assertEquals(adminRole, fetchedRole.get());
  }

  /** Verifies a nonexistent role returns an empty optional. */
  @Test
  void givenNonExistentRoleWhenFindByAuthorityThenReturnEmptyOptional() {
    Optional<Role> fetchedRole = roleRepository.findByAuthority("NONEXISTENTROLE");

    assertNotNull(fetchedRole);
    assertTrue(fetchedRole.isEmpty());
  }
}
