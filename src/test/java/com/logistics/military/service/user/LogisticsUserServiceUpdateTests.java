package com.logistics.military.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.dto.UserUpdateRequestDto;
import com.logistics.military.exception.UnauthorizedOperationException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.service.LogisticsUserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class.
 *
 * <p>This test class verifies the functionality of the {@code updateUser} method, focusing on
 * successful operations, edge cases, and error handling.</p>
 *
 * <h2>Test Scenarios</h2>
 * <ul>
 *   <li>
 *     <strong>Successful Update:</strong> Ensures that a user's details are updated successfully
 *     when valid input is provided.
 *   </li>
 *   <li>
 *     <strong>Unauthorized Operation:</strong> Verifies that attempts to update an admin user
 *     result in an unauthorized operation error.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceUpdateTests {

  @InjectMocks private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;

  UserUpdateRequestDto updateRequestDto;
  Long userId;
  LogisticsUser updatedUser;

  /** Initializes test data before each test case. */
  @BeforeEach
  void setUp() {
    updateRequestDto = new UserUpdateRequestDto(
        "updatedUsername",
        "updatedEmail@example.com"
    );

    userId = 2L;

    updatedUser = new LogisticsUser(
        userId,
        "updatedUsername",
        "password",
        "updatedEmail@example.com",
        LocalDateTime.now(),
        Set.of(new Role("USER"))
    );
  }

  /** Verifies that updating a valid user returns the updated details. */
  @Test
  void givenValidUserUpdateRequestDtoWhenUpdateUserThenUpdatedUser() {
    LogisticsUser user = new LogisticsUser(
        userId,
        "testUser",
        "password",
        "test@example.com",
        LocalDateTime.now(),
        Set.of(new Role("USER"))
    );
    when(logisticsUserRepository.findById(userId)).thenReturn(Optional.of(user));
    when(logisticsUserRepository.save(user)).thenReturn(updatedUser);

    UserResponseDto result = logisticsUserService.updateUser(userId, updateRequestDto);

    verify(logisticsUserRepository, times(1)).findById(userId);
    assertNotNull(result);
    assertEquals(userId, result.getUserId());
    assertEquals(updateRequestDto.getUsername(), result.getUsername());
    assertEquals(updateRequestDto.getEmail(), result.getEmail());
  }

  /** Verifies that updating an admin user throws {@link UnauthorizedOperationException}. */
  @Test
  void givenAdminUserIdWhenUpdateUserThenThrowsUnauthorizedOperationException() {
    Long adminUserId = 1L;
    LogisticsUser adminUser = mock(LogisticsUser.class);
    when(adminUser.hasRole("ADMIN")).thenReturn(true);

    when(logisticsUserRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

    UnauthorizedOperationException exception = assertThrows(UnauthorizedOperationException.class,
        () -> logisticsUserService.updateUser(adminUserId, updateRequestDto));

    verify(logisticsUserRepository, times(1)).findById(adminUserId);
    verify(logisticsUserRepository, never()).save(any(LogisticsUser.class));
    assertNotNull(exception);
    assertEquals(
        String.format("Unauthorized user cannot update admin user with id %d", adminUserId),
        exception.getMessage());
    assertEquals(adminUserId, exception.getId());
  }
}
