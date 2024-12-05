package com.logistics.military.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.dto.AuthTokensDto;
import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.exception.UserAlreadyExistsException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.security.TokenService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link AuthenticationService} class, verifying user authentication and
 * registration functionality.
 *
 * <p>This test class ensures the correct behavior of authentication and registration methods
 * in the {@link AuthenticationService} by mocking dependencies and simulating different
 * user login and registration scenarios.
 * </p>
 *
 * <p>Test cases include:
 * <ul>
 *   <li><strong>Successful user registration:</strong> Validates that a new user can be
 *   successfully registered and saved in the system by the {@code registerNewUser} method.</li>
 *   <li><strong>Successful user login:</strong> Verifies that valid credentials produce JWT tokens
 *   and return a populated {@link AuthTokensDto}.</li>
 *   <li><strong>Authentication failure:</strong> Tests that invalid credentials return an empty
 *   {@code AuthTokensDto} with no user information.</li>
 *   <li><strong>Non-existing user login:</strong> Ensures that attempting to log in with
 *   a non-existent username returns an empty {@code AuthTokensDto}.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthenticationServiceTests {

  @InjectMocks private AuthenticationService authenticationService;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtAuthenticationConverter jwtAuthenticationConverter;
  @Mock private JwtDecoder jwtDecoder;
  @Mock private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private TokenService tokenService;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());
  private UserRequestDto validUserRequest;
  private LogisticsUser logisticsUser;
  private Authentication auth;
  private LogisticsUserDto logisticsUserDto;
  private Map<String, String> tokens;

  /**
   * Sets up test data and mock objects before each test case.
   *
   * <p>This method includes the details for a {@link UserRequestDto}, {@link LogisticsUserDto},
   * and {@link LogisticsUser} objects. This represents everything needed to mock a user in the
   * system.
   * </p>
   *
   * <p>A {@link UsernamePasswordAuthenticationToken} is set up to simulate user credentials
   * for authentication purposes. As well as a map of token strings (access and refresh tokens)
   * to simulate a successful login response.</p>
   */
  @BeforeEach
  void setUp() {
    validUserRequest = new UserRequestDto("validUser", "validPassword", "valid@email.com");

    Role userRole = new Role("ROLE_USER");
    logisticsUser = new LogisticsUser(
        2L,
        "validUser",
        "validPassword",
        "valid@email.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    logisticsUserDto = new LogisticsUserDto(
        2L,
        "validUser",
        "valid@email.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    auth = new UsernamePasswordAuthenticationToken("validUser", "validPassword");

    tokens = Map.of("accessToken", "validAccessToken", "refreshToken", "validRefreshToken");
  }

  /**
   * Test case for when a valid request DTO is provided to the service. The service should return
   * the {@link LogisticsUserDto} object it received from the {@link LogisticsUserService}.
   */
  @Test
  void givenValidRequestWhenRegisterNewUserThenReturnUserDto() {
    UserRequestDto userRequestDto = new UserRequestDto();
    LogisticsUserDto expectedUserDto = new LogisticsUserDto();

    when(logisticsUserService.createAndSaveUser(userRequestDto)).thenReturn(expectedUserDto);

    LogisticsUserDto result = authenticationService.registerNewUser(userRequestDto);

    assertEquals(expectedUserDto, result, "The returned user should match the expected user DTO");
    verify(logisticsUserService, times(1)).createAndSaveUser(userRequestDto);
  }

  /**
   * Test case for when a valid request DTO is provided to the service but contains information
   * of a user that already exists. The service should then a {@link UserAlreadyExistsException} to
   * be handled by the global exception handler.
   */
  @Test
  void givenExistingUserWhenRegisterNewUserThenThrowUserAlreadyExistsException() {
    validUserRequest.setUsername("ExistingUser");
    when(authenticationService.registerNewUser(validUserRequest))
        .thenThrow(new UserAlreadyExistsException("User already exists."));

    assertThrows(UserAlreadyExistsException.class,
        () -> authenticationService.registerNewUser(validUserRequest),
        "Expected registerNewUser to throw an exception for an existing user");

    verify(logisticsUserService, times(1))
        .createAndSaveUser(validUserRequest);
  }

  /**
   * Test case for when valid credentials passed to the service. The service should return with a
   * {@link AuthTokensDto} object containing access and refresh JWT tokens as well as a
   * {@link LogisticsUserDto} containing the user's details.
   */
  @Test
  void givenValidCredentialsWhenLoginUserThenReturnAuthTokensDto() {
    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
    when(tokenService.generateTokens(auth)).thenReturn(tokens);
    when(logisticsUserRepository.findByUsername("validUser"))
        .thenReturn(Optional.of(logisticsUser));
    when(logisticsUserService.mapToUserDto(logisticsUser)).thenReturn(logisticsUserDto);

    AuthTokensDto result = authenticationService.loginUser(validUserRequest);

    assertEquals("validAccessToken", result.getAccessToken());
    assertEquals("validRefreshToken", result.getRefreshToken());
    assertEquals(logisticsUserDto, result.getLogisticsUserDto());
  }

  /**
   * Test case for when an invalid credentials is provided to the service. The service should return
   * an empty {@link AuthTokensDto} due to authentication failure.
   */
  @Test
  void givenInvalidCredentialsWhenLoginUserThenReturnEmptyAutoTokensDto() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new AuthenticationException("Invalid credentials") {});

    AuthTokensDto result = authenticationService.loginUser(validUserRequest);

    assertEquals("", result.getAccessToken());
    assertEquals("", result.getRefreshToken());
    assertNull(result.getLogisticsUserDto());
  }

  /**
   * Test case for when a non-existent username is provided to the server. The service should return
   * an empty {@link AuthTokensDto} due to the user not existing in the database.
   */
  @Test
  void givenNonExistingUserWhenLoginUserThenReturnEmptyAutoTokensDto() {
    validUserRequest.setUsername("NonExistingUser");

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
    when(logisticsUserRepository.findByUsername("NonExistingUser"))
        .thenThrow(new NoSuchElementException());

    AuthTokensDto result = authenticationService.loginUser(validUserRequest);

    assertEquals("", result.getAccessToken());
    assertEquals("", result.getRefreshToken());
    assertNull(result.getLogisticsUserDto());
  }
}
