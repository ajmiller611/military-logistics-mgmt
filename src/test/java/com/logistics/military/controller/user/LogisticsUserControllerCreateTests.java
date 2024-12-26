package com.logistics.military.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.military.config.AppConfig;
import com.logistics.military.config.SecurityConfig;
import com.logistics.military.controller.LogisticsUserController;
import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.LogisticsUserService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link LogisticsUserController} class, focusing on the create user endpoint.
 *
 * <p>This test class ensures the correct behavior of the {@code LogisticsUserController}'s
 * user registration functionality. It validates proper handling of successful requests,
 * input validation, logging behavior, and error responses for various scenarios.</p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Successful User Registration:</strong> Confirms that valid user details
 *     result in a successful response (status 201), with correct headers and body containing
 *     the created user's details.
 *   </li>
 *   <li>
 *     <strong>Logging Behavior:</strong> Verifies that request and response logs are correctly
 *     generated, including key details like the user ID, username, and email.
 *   </li>
 *   <li>
 *     <strong>Validation for Invalid Fields:</strong> Tests multiple scenarios where
 *     the request contains invalid field values (e.g., null, empty, or improperly formatted
 *     username, password, or email) and ensures that appropriate error responses (status 400)
 *     are returned with descriptive messages.
 *   </li>
 *   <li>
 *     <strong>Handling Unknown Fields:</strong> Validates that requests with unrecognized
 *     fields result in a bad request response (status 400) with relevant error messages.
 *   </li>
 *   <li>
 *     <strong>Edge Cases for Special Characters:</strong> Confirms that the endpoint
 *     correctly handles email addresses with valid but uncommon special characters.
 *   </li>
 * </ul>
 */
@WebMvcTest(LogisticsUserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
class LogisticsUserControllerCreateTests {

  @InjectMocks private LogisticsUserController logisticsUserController;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private LogisticsUserService logisticsUserService;
  @MockBean private TokenService tokenService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private LogisticsUserRepository logisticsUserRepository;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  UserRequestDto requestDto;
  String validJson;
  LogisticsUserDto testUser;

  /** Sets up test data for use in each test */
  @BeforeEach
  void setUp() throws JsonProcessingException {
    requestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );
    validJson = objectMapper.writeValueAsString(requestDto);

    Role userRole = new Role("USER");
    testUser = new LogisticsUserDto(
        2L,
        "testUser",
        "test@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );
  }

  /**
   * Test that the /users endpoint will log a received request and its response.
   */
  @Test
  void givenRequestReceivedWhenRegisterUserThenLogRequestAndResponse() throws Exception {
    when(logisticsUserService.createAndSaveUser(any(UserRequestDto.class))).thenReturn(testUser);

    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserController.class)) {
      // Mock the post request to /users
      mockMvc.perform(post("/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(validJson))
          .andExpect(status().isCreated());

      // Verify the log entry of received request
      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Endpoint /users received POST request:")
                  && log.contains(testUser.getUsername())
                  && log.contains(testUser.getEmail()));

      // Verify the log entry of response with key data points
      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Endpoint /users response:")
                  && log.contains(testUser.getUserId().toString())
                  && log.contains(testUser.getUsername())
                  && log.contains(testUser.getEmail()));
    }
  }

  /**
   * Test that the /users endpoint will respond with a status of 201 Created,
   * the location of the created user, and the created user's details.
   */
  @Test
  void givenValidUserDetailsWhenRegisterUserThenReturnCreatedResponse() throws Exception {
    // Set up the clock to return the fixed timestamp
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(logisticsUserService.createAndSaveUser(any(UserRequestDto.class))).thenReturn(testUser);

    // Mock the post request to /users and verify the response
    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+")))
        .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
        .andExpect(jsonPath("$.username").value(testUser.getUsername()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()));

    // Test valid special characters in email
    requestDto.setEmail("te.st-User_name+@ex-am..pl.e2.com");
    testUser.setEmail("te.st-User_name+@ex-am..pl.e2.com");
    String validSpecialCharacterEmailJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validSpecialCharacterEmailJson))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+")))
        .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
        .andExpect(jsonPath("$.username").value(testUser.getUsername()))
        .andExpect(jsonPath("$.email").value(testUser.getEmail()));
  }

  /**
   * Test the /users endpoint will respond with 400 Bad Request
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

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Unrecognized field named 'userId'."));
  }

  /**
   * Test the /users endpoint will respond with 400 Bad Request when the username field
   * has different types of invalid values.
   */
  @Test
  void givenInvalidUsernameWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test null username
    requestDto.setUsername(null);
    String nullUsernameJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(nullUsernameJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.username")
            .value("Username is required"));

    // Test empty username
    requestDto.setUsername("");
    String emptyUsernameJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emptyUsernameJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.username")
            .value("Username is required"));

    // Test username is too short
    requestDto.setUsername("t");
    String shortUsernameJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(shortUsernameJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.username")
            .value("Username must be between 3 and 20 characters"));

    // Test username is too long
    requestDto.setUsername("testUsernameIsTooLong");
    String tooLongUsernameJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(tooLongUsernameJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.username")
            .value("Username must be between 3 and 20 characters"));
  }

  /**
   * Test the /users endpoint will respond with 400 Bad Request when the password field
   * has different types of invalid values.
   */
  @Test
  void givenInvalidPasswordWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test password is null
    requestDto.setPassword(null);
    String nullPasswordJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(nullPasswordJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.password")
            .value("Password is required"));

    // Test password is empty
    requestDto.setPassword("");
    String emptyPasswordJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emptyPasswordJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.password")
            .value("Password is required"));

    // Test password is too short
    requestDto.setPassword("pass");
    String tooShortPasswordJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(tooShortPasswordJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.password")
            .value("Password must be at least 8 characters"));
  }

  /**
   * Test the /users endpoint will respond with 400 Bad Request when the email field
   * has different types of invalid values.
   */
  @Test
  void givenInvalidEmailWhenRegisterUserThenReturnBadRequest() throws Exception {
    // Test email with null value
    requestDto.setEmail(null);
    String nullEmailJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(nullEmailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Email is required"));


    // Test email is empty
    requestDto.setEmail("");
    String emptyEmailJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emptyEmailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Email is required"));

    // Test email missing @ symbol
    requestDto.setEmail("testexample.com");
    String missingAtSymbolJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(missingAtSymbolJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Email invalid. Missing '@' symbol"));

    // Test username of email is invalid
    requestDto.setEmail("test!@example.com");
    String invalidJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Username of Email is invalid."
                + " Only letters, digits, '+', '_', '.', and '-' are valid."));

    // Test email missing period for domain extension
    requestDto.setEmail("test@example");
    String missingPeriodForDomainExtensionJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(missingPeriodForDomainExtensionJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Domain extension is missing (no period)."));

    // Test email missing domain extension
    requestDto.setEmail("test@example.");
    String missingDomainExtension = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(missingDomainExtension))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Domain extension is missing."));

    // Test domain part of email is invalid with invalid character
    requestDto.setEmail("test@exam*ple.com");
    String invalidDomainJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidDomainJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Domain of Email is invalid."
                + " Only letters, digits, '.', and '-' are valid."));

    // Test domain part of email
    requestDto.setEmail("test@ex.am.p*le.com");
    String invalidDomainWhenMultiplePeriodsJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidDomainWhenMultiplePeriodsJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Domain of Email is invalid."
                + " Only letters, digits, '.', and '-' are valid."));

    // Test top-level domain is invalid with one character
    requestDto.setEmail("test@example.c");
    String oneCharacterDomainExtensionJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(oneCharacterDomainExtensionJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Domain extension is invalid."
                + " Only letters are valid and must be at least 2 characters."));

    // Test top-level domain is invalid with invalid character
    requestDto.setEmail("test@example.co*m");
    String invalidDomainExtensionJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidDomainExtensionJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.data.email")
            .value("Domain extension is invalid."
                + " Only letters are valid and must be at least 2 characters."));
  }
}
