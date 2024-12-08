package com.logistics.military.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.dto.UserUpdateRequestDto;
import com.logistics.military.exception.UserCreationException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.service.LogisticsUserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class, designed to validate its update
 * functionalities, edge cases, and error handling mechanisms.
 *
 * <p>The tests cover operations such as:
 * <ul>
 *   <li>Updating a user's details, including handling cases for admin users and non-existent IDs.
 *   </li>
 *   <li>Handling of database exceptions.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceUpdateTests {

  @InjectMocks private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  UserUpdateRequestDto updateRequestDto;
  Long userId;
  LogisticsUser user;
  LogisticsUser updatedUser;

  /**
   * Sets up test data for use before each test case.
   *
   * <p>This method includes details for a {@link LogisticsUser} object and populated page of users.
   * This represents the objects needed to mock a user and page of users to use during tests.
   * </p>
   */
  @BeforeEach
  void setUp() {
    updateRequestDto = new UserUpdateRequestDto(
        "updatedUsername",
        "updatedEmail@example.com"
    );

    userId = 2L;
    Role userRole = new Role("USER");
    user = new LogisticsUser(
        2L,
        "testUser",
        "password",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    updatedUser = new LogisticsUser(
        2L,
        "updatedUsername",
        "password",
        "updatedEmail@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );
  }

  /**
   * Verifies that a valid ID and {@link UserUpdateRequestDto} object returns an updated
   * {@link LogisticsUser} object.
   */
  @Test
  void givenValidUserUpdateRequestDtoWhenUpdateUserThenUpdatedUser() {
    when(logisticsUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(logisticsUserRepository.save(any(LogisticsUser.class))).thenReturn(updatedUser);

    Optional<LogisticsUser> result = logisticsUserService.updateUser(userId, updateRequestDto);

    verify(logisticsUserRepository, times(1)).findById(userId);
    assertNotNull(result);
    assertTrue(result.isPresent());
    assertEquals(updateRequestDto.getUsername(), result.get().getUsername());
    assertEquals(updateRequestDto.getEmail(), result.get().getEmail());
  }

  /**
   * Verifies that non-existing user returns an empty {@link Optional}.
   */
  @Test
  void givenNonExistingUserWhenUpdateUserThenReturnEmptyOptional() {
    when(logisticsUserRepository.findById(userId)).thenReturn(Optional.empty());

    Optional<LogisticsUser> result = logisticsUserService.updateUser(userId, updateRequestDto);

    verify(logisticsUserRepository, times(1)).findById(userId);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Verifies that a valid ID of an admin user returns an empty {@link Optional}.
   */
  @Test
  void givenAdminUserIdWhenUpdateUserThenReturnEmptyOptional() {
    Long adminUserId = 1L;
    Role adminRole = new Role("ADMIN");
    LogisticsUser adminUser = new LogisticsUser(
        adminUserId,
        "admin",
        "password",
        "admin@example.com",
        fixedTimestamp,
        Set.of(adminRole)
    );
    when(logisticsUserRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

    Optional<LogisticsUser> result = logisticsUserService.updateUser(adminUserId, updateRequestDto);

    verify(logisticsUserRepository, times(1)).findById(adminUserId);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Verifies that a database error then a {@link UserCreationException} is thrown.
   */
  @Test
  void givenDatabaseErrorWhenUpdateUserThenThrowUserCreationException() {
    when(logisticsUserRepository.findById(userId)).thenReturn(Optional.of(user));

    DataAccessException mockDataAccessException = mock(DataAccessException.class);
    when(mockDataAccessException.getMessage()).thenReturn("Database save error");
    when(logisticsUserRepository.save(any(LogisticsUser.class)))
        .thenThrow(mockDataAccessException);

    UserCreationException exception = assertThrows(UserCreationException.class,
        () -> logisticsUserService.updateUser(userId, updateRequestDto),
        "Expected updateUser to throw UserCreationException when error saving"
            + " to the database occurs.");

    assertEquals("An error occurred while saving the user to the database",
        exception.getMessage());
    assertNotNull(exception.getCause());
    assertInstanceOf(DataAccessException.class, exception.getCause());
    assertEquals("Database save error", exception.getCause().getMessage());

    reset(logisticsUserRepository);

    when(logisticsUserRepository.findById(userId)).thenReturn(Optional.of(user));

    IllegalArgumentException mockException = mock(IllegalArgumentException.class);
    when(mockException.getMessage()).thenReturn("Database save error");
    when(logisticsUserRepository.save(any(LogisticsUser.class)))
        .thenThrow(mockException);

    exception = assertThrows(UserCreationException.class,
        () -> logisticsUserService.updateUser(userId, updateRequestDto),
        "Expected updateUser to throw UserCreationException when error saving"
            + " to the database occurs.");

    assertEquals("An unexpected error occurred while saving the user to the database",
        exception.getMessage());
    assertNotNull(exception.getCause());
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    assertEquals("Database save error", exception.getCause().getMessage());
  }
}
