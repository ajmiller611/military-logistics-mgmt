package com.logistics.military.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.exception.UnauthorizedOperationException;
import com.logistics.military.exception.UserDeletionException;
import com.logistics.military.exception.UserNotFoundException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.service.LogisticsUserService;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class.
 *
 * <p>This test class validates the {@code deleteUser} method of the {@code LogisticsUserService},
 * focusing on its ability to handle various deletion scenarios, enforce business rules, and manage
 * errors effectively.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Successful User Deletion:</strong> Verifies that a valid user ID successfully deletes
 *     the corresponding user and interacts with the repository as expected.
 *   </li>
 *   <li>
 *     <strong>Handling Non-Existent Users:</strong> Ensures that attempting to delete a user with
 *     an ID that does not exist results in a {@link UserNotFoundException}, with appropriate error
 *     messages and no deletion operation.
 *   </li>
 *   <li>
 *     <strong>Protection Against Unauthorized Operations:</strong> Confirms that attempting to
 *     delete a user with the "ADMIN" role results in an {@link UnauthorizedOperationException},
 *     preventing deletion.
 *   </li>
 *   <li>
 *     <strong>Database Integrity Constraint Violations:</strong> Validates that deleting a user
 *     whose records violate foreign key constraints throws a {@link UserDeletionException}, with
 *     meaningful error details.
 *   </li>
 *   <li>
 *     <strong>Repository Interaction:</strong> Ensures that the method correctly interacts with
 *     the {@link LogisticsUserRepository} by invoking the appropriate find and delete operations
 *     under various scenarios.
 *   </li>
 *   <li>
 *     <strong>Exception Messages:</strong> Verifies that exceptions thrown include informative
 *     and user-friendly messages for easier debugging and error reporting.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceDeleteTests {

  @InjectMocks private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  private static final Long VALID_USER_ID = 2L;
  LogisticsUser testUser;

  /**
   * Set up a new test user object for use in each test.
   */
  @BeforeEach
  void setUp() {
    testUser = new LogisticsUser();
    testUser.setUserId(VALID_USER_ID);
    testUser.setAuthorities(Set.of(new Role("USER")));
  }

  /**
   * Verifies that a valid user id will delete the user of that id.
   */
  @Test
  void givenValidUserIdWhenDeleteUserThenUserIsDeleted() {
    when(logisticsUserRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));

    logisticsUserService.deleteUser(VALID_USER_ID);

    verify(logisticsUserRepository, times(1)).findById(VALID_USER_ID);
    verify(logisticsUserRepository, times(1)).deleteById(VALID_USER_ID);
  }

  /**
   * Verifies that non-existing user throws a {@link UserNotFoundException}.
   */
  @Test
  void givenNonExistingUserWhenDeleteUserThenThrowUserNotFoundException() {
    Long nonExistingUserId = 3L;
    when(logisticsUserRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

    UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
        logisticsUserService.deleteUser(nonExistingUserId));

    assertNotNull(exception);
    assertEquals("User with id 3 does not exist", exception.getMessage());
    verify(logisticsUserRepository, never()).deleteById(any(Long.class));
  }

  /**
   * Verifies that a user id of an admin user throws an {@link UnauthorizedOperationException}.
   */
  @Test
  void givenAdminUserIdWhenDeleteUserThenThrowUnauthorizedOperationException() {
    testUser.setAuthorities(Set.of(new Role("ADMIN")));
    when(logisticsUserRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));

    UnauthorizedOperationException exception = assertThrows(UnauthorizedOperationException.class,
        () -> logisticsUserService.deleteUser(VALID_USER_ID));

    assertNotNull(exception);
    assertEquals("Unauthorized user cannot delete admin user with id 2", exception.getMessage());
    verify(logisticsUserRepository, never()).deleteById(any(Long.class));
  }

  /**
   * Verifies that a deletion that would violate database constraints throws a
   * {@link UserDeletionException}.
   */
  @Test
  void givenDeletionThatViolatesDatabaseConstraintsWhenDeleteUserThenThrowUserDeletionException() {
    when(logisticsUserRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));
    doThrow(DataIntegrityViolationException.class)
        .when(logisticsUserRepository).deleteById(VALID_USER_ID);

    UserDeletionException exception = assertThrows(UserDeletionException.class, () ->
        logisticsUserService.deleteUser(VALID_USER_ID));

    assertNotNull(exception);
    assertEquals("The entity has foreign key constraints", exception.getMessage());
    verify(logisticsUserRepository, times(1)).findById(VALID_USER_ID);
    verify(logisticsUserRepository, times(1)).deleteById(VALID_USER_ID);
  }
}
