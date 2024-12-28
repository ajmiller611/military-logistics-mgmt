package com.logistics.military.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the {@link LogisticsUserRepository} class.
 *
 * <p>This test class validates the integration between the {@code LogisticsUserRepository} and
 * the underlying database. It ensures that repository methods operate as expected, performing
 * accurate queries and handling edge cases such as empty results.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Query Accuracy:</strong> Confirms the correctness of repository methods for
 *     retrieving users by username and filtering users based on roles.
 *   </li>
 *   <li>
 *     <strong>Edge Case Handling:</strong> Verifies behavior when searching for nonexistent
 *     usernames, ensuring the methods return appropriate results (e.g., empty {@code Optional}).
 *   </li>
 *   <li>
 *     <strong>Pagination Support:</strong> Validates that paginated queries return the correct
 *     page of results, with accurate metadata such as total pages and elements.
 *   </li>
 *   <li>
 *     <strong>Role-Based Filtering:</strong> Ensures that users with specific roles (e.g., "ADMIN")
 *     are correctly excluded based on the repository method parameters.
 *   </li>
 *   <li>
 *     <strong>Data Initialization:</strong> Utilizes utility methods to create database states
 *     with varying user-role combinations, allowing comprehensive test scenarios.
 *   </li>
 *   <li>
 *     <strong>Empty Data Handling:</strong> Confirms that repository methods return appropriate
 *     results (e.g., empty pages) when no data matches the query conditions.
 *   </li>
 * </ul>
 */
@ActiveProfiles("integration")
@DataJpaTest
class LogisticsUserRepositoryIntegrationTests {

  @Autowired private LogisticsUserRepository logisticsUserRepository;
  @Autowired private RoleRepository roleRepository;

  private Role adminRole;
  private Role userRole;

  /** Verifies an existent username in the database finds the existent user. */
  @Test
  void givenExistentUsernameWhenFindByUsernameThenReturnUser() {
    initializeRoles(false, true);
    LogisticsUser user = new LogisticsUser(
        null,
        "testUser",
        "password",
        "test@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );
    logisticsUserRepository.save(user);

    Optional<LogisticsUser> fetchedUser = logisticsUserRepository.findByUsername("testUser");

    assertTrue(fetchedUser.isPresent());
    assertEquals(user.getUsername(), fetchedUser.get().getUsername());
  }

  /** Verifies a nonexistent username in the database returns an empty Optional. */
  @Test
  void givenNonExistentUsernameWhenFindByUsernameThenReturnEmptyOptional() {
    Optional<LogisticsUser> fetchedUser =
        logisticsUserRepository.findByUsername("nonexistentUsername");

    assertTrue(fetchedUser.isEmpty());
  }

  /** Verifies users with admin role are excluded from the results of the annotated query. */
  @Test
  void givenRoleToExcludeWhenFindAllWithoutRoleThenReturnUsersWithoutExcludedRole() {
    initializeRoles(true, true);
    initializeDatabaseWithUsersAndAdmin(5, 2);

    Pageable pageable = PageRequest.of(0, 10);
    Page<LogisticsUser> fetchedPagedUsers =
        logisticsUserRepository.findAllWithoutRole(pageable, adminRole);

    assertNotNull(fetchedPagedUsers);
    assertEquals(0, fetchedPagedUsers.getNumber());
    assertEquals(1, fetchedPagedUsers.getTotalPages());
    assertEquals(5, fetchedPagedUsers.getTotalElements());
    assertEquals(5, fetchedPagedUsers.getContent().size());
    assertTrue(fetchedPagedUsers.getContent().stream()
        .noneMatch(user -> user.getAuthorities().contains(adminRole)));
  }

  /** Verifies when only admin users exist in the database then an empty page is returned. */
  @Test
  void givenOnlyAdminUsersExistWhenFindAllWithoutRoleThenReturnEmptyPage() {
    initializeRoles(true, true);
    initializeDatabaseWithUsersAndAdmin(0, 5);

    Pageable pageable = PageRequest.of(0, 10);
    Page<LogisticsUser> fetchedPagedUsers =
        logisticsUserRepository.findAllWithoutRole(pageable, adminRole);

    assertNotNull(fetchedPagedUsers);
    assertTrue(fetchedPagedUsers.isEmpty());
    assertEquals(0, fetchedPagedUsers.getNumber());
    assertEquals(0, fetchedPagedUsers.getTotalPages());
    assertEquals(0, fetchedPagedUsers.getTotalElements());
    assertEquals(0, fetchedPagedUsers.getContent().size());
  }

  /** Verifies when no users exist then an empty page is returned. */
  @Test
  void givenNoUsersWhenFindAllWithoutRoleThenReturnEmptyPage() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<LogisticsUser> fetchedPagedUsers =
        logisticsUserRepository.findAllWithoutRole(pageable, adminRole);

    assertNotNull(fetchedPagedUsers);
    assertTrue(fetchedPagedUsers.isEmpty());
  }

  /** Verifies when multiple pages of users exists then the requested page is the correct page. */
  @Test
  void givenMultipleUserPagesWhenFindAllWithoutRoleThenReturnCorrectPage() {
    initializeRoles(true, true);
    initializeDatabaseWithUsersAndAdmin(10, 2);

    Pageable pageable = PageRequest.of(1, 5);
    Page<LogisticsUser> fetchedPagedUsers =
        logisticsUserRepository.findAllWithoutRole(pageable, adminRole);

    assertNotNull(fetchedPagedUsers);
    assertEquals(1, fetchedPagedUsers.getNumber());
    assertEquals(2, fetchedPagedUsers.getTotalPages());
    assertEquals(10, fetchedPagedUsers.getTotalElements());
    assertEquals(5, fetchedPagedUsers.getContent().size());
    assertTrue(fetchedPagedUsers.getContent().stream()
        .noneMatch(user -> user.getAuthorities().contains(adminRole)));

    assertEquals("testUser6", fetchedPagedUsers.getContent().getFirst().getUsername());
    assertEquals("testUser7", fetchedPagedUsers.getContent().get(1).getUsername());
    assertEquals("testUser8", fetchedPagedUsers.getContent().get(2).getUsername());
    assertEquals("testUser9", fetchedPagedUsers.getContent().get(3).getUsername());
    assertEquals("testUser10", fetchedPagedUsers.getContent().get(4).getUsername());
  }

  /** Verify deleting an entry in the user table doesn't affect the roles table. */
  @Test
  void givenExistingUserWhenUserDeletedThenRoleTableUnaffected() {
    initializeRoles(true, false);
    initializeDatabaseWithUsersAndAdmin(0, 1);

    // Ensure the role exists in the database before deleting the user
    int roleId = adminRole.getRoleId();
    assertTrue(roleRepository.existsById(roleId));

    // Get the created user and delete the user from the database
    LogisticsUser user = logisticsUserRepository.findByUsername("admin1").orElseThrow();
    logisticsUserRepository.delete(user);

    // Ensure the user has been deleted
    Optional<LogisticsUser> deletedUser = logisticsUserRepository.findById(user.getUserId());
    assertFalse(deletedUser.isPresent());

    // Verify the role exists in the database after deleting the user
    assertTrue(roleRepository.existsById(roleId));
  }

  /**
   * Utility method to create variable number of users and admin in the database to have a desired
   * database state.
   *
   * @param userCount the number of users to exist in the database
   * @param adminCount the number of admin users to exist in the database
   */
  private void initializeDatabaseWithUsersAndAdmin(int userCount, int adminCount) {
    // Create admin users
    for (int i = 1; i <= adminCount; i++) {
      LogisticsUser user = new LogisticsUser(
          null,
          "admin" + i,
          "password",
          "admin" + i + "@example.com",
          LocalDateTime.now(),
          Set.of(adminRole)
      );
      logisticsUserRepository.save(user);
    }

    // Create users
    for (int i = 1; i <= userCount; i++) {
      LogisticsUser user = new LogisticsUser(
          null,
          "testUser" + i,
          "password",
          "test" + i + "@example.com",
          LocalDateTime.now(),
          Set.of(userRole)
      );
      logisticsUserRepository.save(user);
    }
  }

  /**
   * Initializes the specified roles by saving them to the database. This utility method optimizes
   * test runtime by limiting role creation to only those required for the test case.
   *
   * @param admin {@code true} to create and save the admin role; {@code false} otherwise.
   * @param user {@code true} to create and save the user role; {@code false} otherwise.
   */
  private void initializeRoles(boolean admin, boolean user) {
    if (admin) {
      adminRole = roleRepository.save(new Role("ADMIN"));
    }
    if (user) {
      userRole = roleRepository.save(new Role("USER"));
    }
  }
}
