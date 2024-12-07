package com.logistics.military.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.exception.UserAlreadyExistsException;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class, designed to validate its creation and
 * authentication functionalities, edge cases, and error handling mechanisms.
 *
 * <p>The tests cover operations such as:
 * <ul>
 *   <li>Mapping between domain model ({@link LogisticsUser}) and ({@link LogisticsUserDto}).</li>
 *   <li>Creating and saving users, including validation of inputs, role assignments,
 *       and exception handling.</li>
 *   <li>Loading user details for authentication, ensuring the correct handling of existing
 *       and non-existing users.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceCreateTests {

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

  UserRequestDto userRequestDto;
  Role userRole;
  LogisticsUser user;

  /**
   * Sets up test data for use before each test case.
   *
   * <p>This method includes details for a {@link UserRequestDto} and {@link LogisticsUser} objects.
   * This represents the objects needed to mock a user and pass user data in the system.
   * </p>
   */
  @BeforeEach
  void setUp() {
    userRequestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );

    userRole = new Role("USER");
    user = new LogisticsUser(
        2L,
        userRequestDto.getUsername(),
        userRequestDto.getPassword(),
        userRequestDto.getEmail(),
        fixedTimestamp,
        Set.of(userRole)
    );
  }

  /**
   * Test case for the utility method of mapping a {@link LogisticsUser} to a
   * {@link LogisticsUserDto}. This test makes sure that the mapping is correctly done.
   */
  @Test
  void givenLogisticsUserWhenMapToUserDtoThenReturnLogisticsUserDto() {
    LogisticsUserDto dto = logisticsUserService.mapToUserDto(user);

    assertNotNull(dto);
    assertEquals(user.getUserId(), dto.getUserId());
    assertEquals(user.getUsername(), dto.getUsername());
    assertEquals(user.getEmail(), dto.getEmail());
    assertEquals(user.getCreatedAt(), dto.getCreatedAt());
    assertEquals(1, dto.getAuthorities().size());
    assertEquals("USER", dto.getAuthorities().iterator().next().getAuthority());
  }

  /**
   * Test case for when a valid request DTO is provided to the service. The service should return a
   * populated {@link LogisticsUserDto} with the newly created user's details.
   */
  @Test
  void givenValidUserRequestDtoWhenCreateAndSaveUserThenReturnLogisticsUserDto() {

    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority(userRole.getAuthority())).thenReturn(Optional.of(userRole));
    when(logisticsUserRepository.save(any(LogisticsUser.class))).thenReturn(user);

    LogisticsUserDto userDto = logisticsUserService.createAndSaveUser(userRequestDto);

    assertNotNull(userDto);
    assertEquals(user.getUserId(), userDto.getUserId());
    assertEquals(user.getUsername(), userDto.getUsername());
    assertEquals(user.getEmail(), userDto.getEmail());
    assertEquals(user.getCreatedAt(), userDto.getCreatedAt());
    assertEquals(user.getAuthorities(), userDto.getAuthorities());
  }

  /**
   * Test case for when an invalid username is in the {@link UserRequestDto}. This shouldn't happen
   * since the username should have gone through validation in the controller. This test verifies
   * the backup check in {@link LogisticsUserService} is working correctly.
   */
  @Test
  void givenInvalidUsernameWhenCreateAndSaveUserThenThrowIllegalArgumentException() {
    userRequestDto.setUsername(null);
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an null username");

    userRequestDto.setUsername("");
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an empty username");
  }

  /**
   * Test case for when an invalid password is in the {@link UserRequestDto}. This shouldn't happen
   * since the password should have gone through validation in the controller. This test verifies
   * the backup check in {@link LogisticsUserService} is working correctly.
   */
  @Test
  void givenInvalidPasswordWhenCreateAndSaveUserThenThrowIllegalArgumentException() {
    userRequestDto.setPassword(null);
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an null password");

    userRequestDto.setPassword("");
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an empty password");
  }

  /**
   * Test case for when an invalid email s in the {@link UserRequestDto}. This shouldn't happen
   * since the email should have gone through validation in the controller. This test verifies
   * the backup check in {@link LogisticsUserService} is working correctly.
   */
  @Test
  void givenInvalidEmailWhenCreateAndSaveUserThenThrowIllegalArgumentException() {
    userRequestDto.setEmail(null);
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an null email");

    userRequestDto.setEmail("");
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an empty email");
  }

  /**
   * Test case for when the username already exists in the database. The service should throw an
   * {@link UserAlreadyExistsException} when trying to create a user that already exists.
   */
  @Test
  void givenExistingUserWhenCreateAndSaveUserThenThrowUserAlreadyExistsException() {
    when(logisticsUserRepository.findByUsername(userRequestDto.getUsername()))
        .thenReturn(Optional.of(new LogisticsUser()));

    assertThrows(UserAlreadyExistsException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an existing user");
  }

  /**
   * Test case for when the 'USER' role can not be found in the database. The service should throw
   * an {@link IllegalStateException} when the 'USER' role does not exist.
   */
  @Test
  void givenRoleNotFoundWhenCreateAndSaveUserThenThrowIllegalStateException() {
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for role not found");
  }

  /**
   * Test case for when an error occurs when trying to save the user to the database. The service
   * should throw a {@link UserCreationException} when a database error occurs.
   */
  @Test
  void givenDatabaseErrorWhenCreateAndSaveUserThenThrowUserCreationException() {
    when(logisticsUserRepository.findByUsername(userRequestDto.getUsername()))
        .thenReturn(Optional.empty());
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.of(userRole));

    DataAccessException mockDataAccessException = mock(DataAccessException.class);
    when(mockDataAccessException.getMessage()).thenReturn("Database save error");
    when(logisticsUserRepository.save(any(LogisticsUser.class)))
        .thenThrow(mockDataAccessException);

    UserCreationException exception = assertThrows(UserCreationException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw UserCreationException when error saving"
        + " to the database occurs.");

    assertEquals("An error occurred while saving the user to the database", exception.getMessage());
    assertNotNull(exception.getCause());
    assertInstanceOf(DataAccessException.class, exception.getCause());
    assertEquals("Database save error", exception.getCause().getMessage());

    reset(logisticsUserRepository);

    IllegalArgumentException mockException = mock(IllegalArgumentException.class);
    when(mockException.getMessage()).thenReturn("Database save error");
    when(logisticsUserRepository.save(any(LogisticsUser.class)))
        .thenThrow(mockException);

    exception = assertThrows(UserCreationException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw UserCreationException when error saving"
            + " to the database occurs.");

    assertEquals("An unexpected error occurred while saving the user to the database",
        exception.getMessage());
    assertNotNull(exception.getCause());
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    assertEquals("Database save error", exception.getCause().getMessage());
  }

  /**
   * Test case for when a user exists and needs to be queried for authentication. The service should
   * return an {@link UserDetails} with the user's details found in the database.
   */
  @Test
  void givenUserExistsWhenLoadUserByUsernameThenReturnUserDetails() {
    String username = "testUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.of(user));

    UserDetails userDetails = logisticsUserService.loadUserByUsername(username);

    assertNotNull(userDetails);
    assertEquals(username, userDetails.getUsername());
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }

  /**
   * Test case for when a user does not exist but is queried for authentication. The service should
   * throw an {@link UsernameNotFoundException}.
   */
  @Test
  void givenUserDoesNotExistsWhenLoadUserByUsernameThenThrowUsernameNotFoundException() {
    String username = "invalidUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
        () -> logisticsUserService.loadUserByUsername(username),
        "Expected loadUserByUsername to throw exception when user does not exist.");
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }
}
