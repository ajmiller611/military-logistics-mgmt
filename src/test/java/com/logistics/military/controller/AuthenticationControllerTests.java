package com.logistics.military.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.military.config.AppConfig;
import com.logistics.military.config.SecurityConfig;
import com.logistics.military.dto.AuthTokensDto;
import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.AuthenticationService;
import com.logistics.military.service.LogisticsUserService;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link AuthenticationController}, validating the functionality
 * of authentication-related endpoints, including registration and login flows.
 *
 * <p>This test class covers both successful and error scenarios for each endpoint,
 * ensuring proper response statuses, response structure, and handling of edge cases.
 * </p>
 *
 * <p>Test cases include:
 * <ul>
 *     <li><b>Register Endpoint:</b> Verifies successful registration and handles invalid input
 *     cases.</li>
 *     <li><b>Login Endpoint:</b> Verifies successful login and invalid login attempts.</li>
 *     <li><b>Refresh Token Endpoint:</b> Verifies responses to valid and invalid refresh tokens.
 *     </li>
 * </ul>
 * </p>
 *
 * <p>Uses {@link MockMvc} to simulate HTTP requests and responses, enabling
 * comprehensive validation of controller behavior without needing to start the full application.
 * </p>
 */
@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
public class AuthenticationControllerTests {

  @InjectMocks private AuthenticationController authenticationController;
  @Autowired private MockMvc mockMvc;
  @MockBean private AuthenticationService authenticationService;
  @MockBean private LogisticsUserService logisticsUserService;
  @MockBean private TokenService tokenService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private LogisticsUserRepository logisticsUserRepository;
  @MockBean private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  /**
   * Test that the /auth/register endpoint will log a received request and its response.
   */
  @Test
  void givenRequestReceivedWhenRegisterUserThenLogRequestAndResponse() throws Exception {
    // Create a test user
    Role userRole = new Role("USER");
    LogisticsUserDto testUser = new LogisticsUserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );
    when(authenticationService.registerNewUser(any(UserRequestDto.class))).thenReturn(testUser);

    try (LogCaptor logCaptor = LogCaptor.forClass(AuthenticationController.class)) {
      String json = """
          {
            "username": "testUser",
            "password": "password",
            "email": "test@example.com"
          }
          """;

      // Mock the post request to /auth/register
      mockMvc.perform(post("/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json));

      // Verify the log entry of received request
      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Endpoint /auth/register received request")
                  && log.contains(testUser.getUsername())
                  && log.contains(testUser.getEmail()));

      // Verify the log entry of response with key data points
      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Endpoint /auth/register response:")
                  && log.contains(testUser.getUserId().toString())
                  && log.contains(testUser.getUsername())
                  && log.contains(testUser.getEmail()));
    }
  }

  /**
   * Test that the /auth/register endpoint will respond with a status of 201 Created,
   * the location of the created user, and the created user's details.
   */
  @Test
  void givenValidUserDetailsWhenRegisterUserThenReturnCreatedResponse() throws Exception {
    // Set up the clock to return the fixed timestamp
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());

    Role userRole = new Role("USER");
    LogisticsUserDto testUser = new LogisticsUserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );
    when(authenticationService.registerNewUser(any(UserRequestDto.class))).thenReturn(testUser);

    String json = """
        {
          "username": "testUser",
          "password": "password",
          "email": "test@example.com"
        }
        """;

    // Mock the post request to /auth/register and verify the response
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+")))
        .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
        .andExpect(jsonPath("$.username").value(testUser.getUsername()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()));

    // Test valid special characters in email
    testUser.setEmail("te.st-User_name+@ex-am..pl.e2.com");
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "te.st-User_name+@ex-am..pl.e2.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+")))
        .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
        .andExpect(jsonPath("$.username").value(testUser.getUsername()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()));
  }

  /**
   * Test the /auth/register endpoint will respond with 400 Bad Request
   * when an unknown field is present in the request.
   */
  @Test
  void givenUnknownFieldWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test an unknown field of userId exists
    String json = """
        {
          "userId": 3,
          "username": "testUser",
          "password": "password",
          "email": "test@example.com"
        }
        """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Invalid request body"))
        .andExpect(jsonPath("$.message").value(containsString("Unrecognized field:")));
  }

  /**
   * Test the /auth/register endpoint will respond with 400 Bad Request when the username field
   * has different types of invalid values.
   */
  @Test
  void givenInvalidUsernameWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test null username
    String json = """
        {
          "username": null,
          "password": "password",
          "email": "test@example.com"
        }
        """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.username")
            .value("Username is required"));

    // Test empty username
    json = """
      {
        "username": "",
        "password": "password",
        "email": "test@example.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.username")
            .value("Username is required"));

    // Test username is too short
    json = """
      {
        "username": "t",
        "password": "password",
        "email": "test@example.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.username")
            .value("Username must be between 3 and 20 characters"));

    // Test username is too long
    json = """
      {
        "username": "testUsernameIsTooLong",
        "password": "password",
        "email": "test@example.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.username")
            .value("Username must be between 3 and 20 characters"));
  }

  /**
   * Test the /auth/register endpoint will respond with 400 Bad Request when the password field
   * has different types of invalid values.
   */
  @Test
  void givenInvalidPasswordWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test password is null
    String json = """
        {
          "username": "testUser",
          "password": null,
          "email": "test@example.com"
        }
        """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.password")
            .value("Password is required"));

    // Test password is empty
    json = """
      {
        "username": "testUser",
        "password": "",
        "email": "test@example.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.password")
            .value("Password is required"));

    // Test password is too short
    json = """
      {
        "username": "testUser",
        "password": "pass",
        "email": "test@example.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.password")
            .value("Password must be at least 8 characters"));
  }

  /**
   * Test the /auth/register endpoint will respond with 400 Bad Request when the email field
   * has different types of invalid values.
   */
  @Test
  void givenInvalidEmailWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test email with null value
    String json = """
        {
          "username": "testUser",
          "password": "password",
          "email": null
        }
        """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Email is required"));


    // Test email is empty
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": ""
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Email is required"));

    // Test email missing @ symbol
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "testexample.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Email invalid. Missing '@' symbol"));

    // Test username of email is invalid
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test!@example.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Username of Email is invalid."
                + " Only letters, digits, '+', '_', '.', and '-' are valid."));

    // Test email missing period for domain extension
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test@example"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Domain extension is missing (no period)."));

    // Test email missing domain extension
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test@example."
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Domain extension is missing."));

    // Test domain part of email is invalid with invalid character
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test@exam*ple.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Domain of Email is invalid."
                + " Only letters, digits, '.', and '-' are valid."));

    // Test domain part of email
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test@ex.am.p*le.com"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Domain of Email is invalid."
                + " Only letters, digits, '.', and '-' are valid."));

    // Test top-level domain is invalid with one character
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test@example.c"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Domain extension is invalid."
                + " Only letters are valid and must be at least 2 characters."));

    // Test top-level domain is invalid with invalid character
    json = """
      {
        "username": "testUser",
        "password": "password",
        "email": "test@example.co*m"
      }
      """;

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details.email")
            .value("Domain extension is invalid."
                + " Only letters are valid and must be at least 2 characters."));
  }

  /**
   * Test that the /auth/login endpoint will log a received request and its response.
   */
  @Test
  void givenRequestReceivedWhenLoginUserThenLogRequestAndResponse() throws Exception {
    Role userRole = new Role("USER");
    LogisticsUserDto testUser = new LogisticsUserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    AuthTokensDto authTokensDto = new AuthTokensDto(
        "access_token_value",
        "refresh_token_value",
        testUser
    );

    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(authTokensDto);

    try (LogCaptor logCaptor = LogCaptor.forClass(AuthenticationController.class)) {
      String json = """
          {
            "username": "testUser",
            "password": "password",
            "email": "test@example.com"
          }
          """;

      mockMvc.perform(post("/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json));

      // Verify the log entry of received request
      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Endpoint /auth/login received request:")
                  && log.contains(testUser.getUsername())
                  && log.contains(testUser.getEmail()));

      // Verify the log entry of the response
      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Endpoint /auth/login response:")
                  && log.contains(testUser.getUserId().toString())
                  && log.contains(testUser.getUsername())
                  && log.contains(testUser.getEmail()));
    }
  }

  /**
   * Test that the /auth/login endpoint will respond with 200 Ok, access and refresh tokens
   * in the form of cookies, and the user's details.
   */
  @Test
  void givenValidCredentialsWhenLoginUserThenReturnTokenCookiesAndUserDetails() throws Exception {
    Role userRole = new Role("USER");
    LogisticsUserDto testUser = new LogisticsUserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    AuthTokensDto authTokensDto = new AuthTokensDto(
        "access_token_value",
        "refresh_token_value",
        testUser
    );

    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(authTokensDto);

    String json = """
        {
          "username": "testUser",
          "password": "password",
          "email": "test@example.com"
        }
        """;

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(cookie().value("access_token", "access_token_value"))
        .andExpect(cookie().value("refresh_token", "refresh_token_value"))
        .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
        .andExpect(jsonPath("$.username").value(testUser.getUsername()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()));

    verify(authenticationService).loginUser(any(UserRequestDto.class));
  }

  /**
   * Test that the /auth/login endpoint will respond with 401 Unauthorized when invalid credentials
   * are received.
   */
  @Test
  void givenInvalidCredentialsWhenLoginUserThenReturnUnauthorizedResponse() throws Exception {
    AuthTokensDto nullAuthTokensDto = new AuthTokensDto(null, null, null);
    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(nullAuthTokensDto);

    String json = """
        {
          "username": "invalidUser",
          "password": "wrongPassword",
          "email": "test@example.com"
        }
        """;

    mockMvc.perform(post("/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isUnauthorized());

    verify(authenticationService).loginUser(any(UserRequestDto.class));

    // Reset the configurations of the Mockito.when method
    reset(authenticationService);

    AuthTokensDto emptyAuthTokensDto = new AuthTokensDto("", "", null);
    when(authenticationService.loginUser(any(UserRequestDto.class))).thenReturn(emptyAuthTokensDto);

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isUnauthorized());

    verify(authenticationService).loginUser(any(UserRequestDto.class));
  }

  /**
   * Test that the /auth/refresh-token endpoint respond with 200 Ok and access and refresh tokens
   * in the form of cookies.
   */
  @Test
  void givenValidRefreshTokenWhenRefreshTokenThenReturnOkResponse() throws Exception {
    Cookie validRefreshTokenCookie = new Cookie("refresh_token", "validToken");

    Jwt validJwt = mock(Jwt.class);
    when(validJwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(60 * 60));
    when(validJwt.getSubject()).thenReturn("testUser");

    when(jwtDecoder.decode("validToken")).thenReturn(validJwt);
    when(tokenService.jwtDecoder()).thenReturn(jwtDecoder);

    Authentication auth = new UsernamePasswordAuthenticationToken(
        "testUser",
        null,
        Collections.emptyList()
    );
    when(tokenService.generateAccessToken(auth)).thenReturn("newAccessToken");
    when(tokenService.generateRefreshToken(auth)).thenReturn("newRefreshToken");

    mockMvc.perform(post("/auth/refresh-token")
        .cookie(validRefreshTokenCookie))
        .andExpect(status().isOk())
        .andExpect(cookie().value("access_token", "newAccessToken"))
        .andExpect(cookie().value("refresh_token", "newRefreshToken"))
        .andExpect(cookie().httpOnly("access_token", true))
        .andExpect(cookie().httpOnly("refresh_token", true))
        .andExpect(cookie().secure("access_token", true))
        .andExpect(cookie().secure("refresh_token", true))
        .andExpect(cookie().maxAge("access_token", 15 * 60))
        .andExpect(cookie().maxAge("refresh_token", 7 * 24 * 60 * 60));
  }

  /**
   * Test that the /auth/refresh-token endpoint respond with 400 Bad Request when
   * missing the refresh token.
   */
  @Test
  void givenMissingRefreshTokenWhenRefreshTokenThenReturnBadRequest() throws Exception {
    mockMvc.perform(post("/auth/refresh-token"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Refresh Token is missing"));
  }

  /**
   * Test that the /auth/refresh-token endpoint responds with 403 Forbidden when
   * expired refresh token is received.
   */
  @Test
  void givenExpiredRefreshTokenWhenRefreshTokenThenReturnForbiddenResponse() throws Exception {
    Cookie expiredRefreshTokenCookie = new Cookie("refresh_token", "expiredToken");

    Jwt expiredJwt = mock(Jwt.class);
    when(expiredJwt.getExpiresAt()).thenReturn(Instant.now().minusSeconds(60));
    when(expiredJwt.getSubject()).thenReturn("testUser");

    when(jwtDecoder.decode("expiredToken")).thenReturn(expiredJwt);
    when(tokenService.jwtDecoder()).thenReturn(jwtDecoder);

    mockMvc.perform(post("/auth/refresh-token")
        .cookie(expiredRefreshTokenCookie))
        .andExpect(status().isForbidden())
        .andExpect(content().string("Refresh token has expired"));
  }

  /**
   * Test that the /auth/refresh-token endpoint responds with 400 Bad Request when
   * invalid refresh token is received.
   */
  @Test
  void givenInvalidRefreshTokenWhenRefreshTokenThenReturnBadRequest() throws Exception {
    Cookie invalidRefreshTokenCookie = new Cookie("refresh_token", "invalidToken");
    when(jwtDecoder.decode("invalidToken")).thenThrow(new JwtException("Invalid token"));
    when(tokenService.jwtDecoder()).thenReturn(jwtDecoder);

    mockMvc.perform(post("/auth/refresh-token")
            .cookie(invalidRefreshTokenCookie))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Invalid token"));
  }
}
