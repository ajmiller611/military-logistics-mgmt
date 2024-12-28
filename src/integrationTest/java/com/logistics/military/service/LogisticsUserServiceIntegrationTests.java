package com.logistics.military.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.dto.UserUpdateRequestDto;
import com.logistics.military.exception.UserAlreadyExistsException;
import com.logistics.military.exception.UserNotFoundException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link LogisticsUserService} class.
 *
 * <p>This test class verifies the functionality in the {@link LogisticsUserService},
 * focusing on database interactions, integration with the {@code @CheckUserExistence} aspect,
 * and exception handling.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>User Creation:</strong> Ensures {@code createAndSaveUser()} saves the user details
 *     including encoding the password and the correct roles are assigned.
 *   </li>
 *   <li>
 *     <strong>Aspect Interception for Existing Users:</strong> Confirms that the
 *     {@code @CheckUserExistence} aspect intercepts the call to {@code createAndSaveUser()}
 *     and properly throws a {@link UserAlreadyExistsException} when a user with the
 *     given username already exists in the database.
 *   </li>
 *   <li>
 *     <strong>Aspect Interception for Nonexistent Users:</strong> Confirms that the
 *     {@code @CheckUserExistence} aspect intercepts the call to {@code getUserById()},
 *     {@code updateUser()}, and {@code deleteUser()} throws a {@link UserNotFoundException}
 *     when attempting to retrieve a nonexistent user.
 *   </li>
 *   <li>
 *     <strong>Exception Propagation:</strong> Ensures that the exception thrown by the aspect is
 *     propagated to the {@code createAndSaveUser} method, satisfying the integration requirements.
 *   </li>
 *   <li>
 *     <strong>Database Integrity:</strong> Verifies that the number of users in the database
 *     remains unchanged when the {@code createAndSaveUser} method is invoked with a duplicate user,
 *     ensuring no unintended side effects occur.
 *   </li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureMockMvc
@Transactional
class LogisticsUserServiceIntegrationTests {

  @Autowired private LogisticsUserService logisticsUserService;
  @Autowired private LogisticsUserRepository logisticsUserRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  Role userRole;

  /** Verify {@code createAndSaveUser()} saves user with encoded password and correct roles. */
  @Test
  void givenValidUserRequestDtoWhenCreateAndSaveUserThenUserSavedProperly() {
    initializeUserRole();
    UserRequestDto userRequestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );

    logisticsUserService.createAndSaveUser(userRequestDto);
    LogisticsUser user = logisticsUserRepository.findByUsername("testUser").orElseThrow();

    assertNotNull(user, "The user should not be null after saving.");
    assertEquals(1L, user.getUserId(), "The user ID should be 1.");
    assertEquals(userRequestDto.getUsername(), user.getUsername(),
        "The username should match the provided username.");
    assertTrue(passwordEncoder.matches(userRequestDto.getPassword(), user.getPassword()),
        "The password should be correctly encoded.");
    assertEquals(userRequestDto.getEmail(), user.getEmail(),
        "The email should match the provided email.");
    assertTrue(user.getCreatedAt().isBefore(LocalDateTime.now()),
        "The creation date should be before the current date.");
    assertTrue(user.getAuthorities().contains(userRole),
        "The user should have the 'USER' role assigned.");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code createAndSaveUser()} and throws a {@link UserAlreadyExistsException} when a user already
   * exists. The exception is propagated to {@code createAndSaveUser()} which satisfies the test.
   */
  @Test
  void givenExistingUserWhenCreateAndSaveUserThenThrowUserAlreadyExistsException() {
    initializeUserRole();
    String existingUsername = "existingUser";
    LogisticsUser existingUser = new LogisticsUser(
        2L,
        existingUsername,
        "password",
        "existing@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );
    logisticsUserRepository.save(existingUser);

    UserRequestDto userRequestDto = new UserRequestDto(
        existingUsername,
        "password",
        "newUser@example.com"
    );

    assertThrows(UserAlreadyExistsException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an existing user");

    Long userCountAfter = logisticsUserRepository.count();
    assertEquals(1, userCountAfter,
        "The number of users in the database should not have changed.");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code getUserById()} and throws a {@link UserNotFoundException} when a user is nonexistent.
   * The exception is propagated to {@code getUserById()} which satisfies the test.
   */
  @Test
  void givenNonExistentUserWhenGetUserByIdThenThrowUserNotFoundException() {
    Long nonExistentId = 2L;
    assertThrows(UserNotFoundException.class,
        () -> logisticsUserService.getUserById(nonExistentId),
        "Expected getUserById to throw a UserNotFoundException for a nonexistent user");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code updateUser()} and throws a {@link UserNotFoundException} when a user is nonexistent.
   * The exception is propagated to {@code updateUser()} which satisfies the test.
   */
  @Test
  void givenNonExistentUserWhenUpdateUserThenThrowUserNotFoundException() {
    Long nonExistentId = 2L;
    UserUpdateRequestDto updateRequestDto = new UserUpdateRequestDto(
        "updatedUsername",
        "updatedEmail@example.com"
    );
    assertThrows(UserNotFoundException.class,
        () -> logisticsUserService.updateUser(nonExistentId, updateRequestDto),
        "Expected updateUser to throw a UserNotFoundException for a nonexistent user");
  }

  /**
   * Verify the {@code @CheckUserExistence} aspect intercepts the call to
   * {@code deleteUser()} and throws a {@link UserNotFoundException} when a user is nonexistent.
   * The exception is propagated to {@code deleteUser()} which satisfies the test.
   */
  @Test
  void givenNonExistentUserWhenDeleteUserThenThrowUserNotFoundException() {
    Long nonExistentId = 2L;
    assertThrows(UserNotFoundException.class,
        () -> logisticsUserService.deleteUser(nonExistentId),
        "Expected deleteUser to throw a UserNotFoundException for a nonexistent user");
  }

  /**
   * Initializes the 'USER' role by saving it to the database. This utility method optimizes
   * test runtime by limiting role creation to only test cases that require the 'USER' role.
   */
  private void initializeUserRole() {
    userRole = roleRepository.save(new Role("USER"));
  }
}