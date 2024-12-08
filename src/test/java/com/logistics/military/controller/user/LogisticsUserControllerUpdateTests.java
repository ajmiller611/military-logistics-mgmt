package com.logistics.military.controller.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.military.config.AppConfig;
import com.logistics.military.config.SecurityConfig;
import com.logistics.military.controller.LogisticsUserController;
import com.logistics.military.dto.UserUpdateRequestDto;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.LogisticsUserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link LogisticsUserController} class.
 *
 * <p>This test class verifies the functionality of the {@code LogisticsUserController}'s
 * update user endpoint, focusing on ensuring the correct handling of valid and
 * invalid user update requests, as well as appropriate error responses for different scenarios.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Valid User Update:</strong> Ensures that a valid user update request results in a
 *     successful response (status 200) with the updated user details.
 *   </li>
 *   <li>
 *     <strong>Validation of User Update Request:</strong> Verifies that invalid user data,
 *     such as an incorrect username or email, results in a proper bad request response (status 400)
 *     with a validation error message.
 *   </li>
 *   <li>
 *     <strong>Invalid User ID Handling:</strong> Confirms that when an invalid user ID is provided,
 *     the controller responds with a bad request (status 400) and an appropriate error message.
 *   </li>
 *   <li>
 *     <strong>Non-Existent User Handling:</strong> Tests that when the service returns an empty
 *     {@link Optional}, indicating that the user does not exist, the controller responds
 *     with a not found (status 404) error and a corresponding error message.
 *   </li>
 *   <li>
 *     <strong>Logging Behavior:</strong> Verifies that the controller logs appropriate messages for
 *     valid and invalid requests, including error logs when an invalid user ID is provided.
 *   </li>
 *   <li>
 *     <strong>Response Structure:</strong> Ensures that the response structure is consistent,
 *     including the status, message, and data fields.
 *   </li>
 * </ul>
 */
@WebMvcTest(LogisticsUserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
class LogisticsUserControllerUpdateTests {

  @InjectMocks private LogisticsUserController logisticsUserController;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private LogisticsUserService logisticsUserService;
  @MockBean private TokenService tokenService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private LogisticsUserRepository logisticsUserRepository;

  Long testUserId;
  UserUpdateRequestDto requestDto;
  String validJson;

  /**
   * Sets up a test user object, test user id, and request DTO for use during
   * 'updateUser' method tests.
   */
  @BeforeEach
  void setUp() throws JsonProcessingException {
    testUserId = 2L;
    Role userRole = new Role("USER");
    LogisticsUser updatedUser = new LogisticsUser(
        testUserId,
        "updatedUsername",
        "password",
        "updatedEmail@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );

    requestDto = new UserUpdateRequestDto("updatedUsername", "updatedEmail@example.com");

    validJson = objectMapper.writeValueAsString(requestDto);

    // Mockito uses strict argument matching so if an argument uses an ArgumentMatchers method
    // then all arguments must be of ArgumentMatchers (e.g. eg() and any()).
    when(logisticsUserService.updateUser(eq(testUserId), any(UserUpdateRequestDto.class)))
        .thenReturn(Optional.of(updatedUser));
  }

  /**
   * Verifies that a valid user with a valid PUT request returns ok (200) with updated user details.
   */
  @Test
  @WithMockUser
  void givenValidRequestDtoWhenUpdateUserThenReturnsSuccessWithUpdatedUserDetails()
      throws Exception {

    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserController.class)) {
      mockMvc.perform(put("/users/2")
              .with(csrf()) // Spring Security enforces CSRF protection for PUT requests.
              .contentType(MediaType.APPLICATION_JSON)
              .content(validJson))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"))
          .andExpect(jsonPath("$.message").value("User updated successfully"))
          .andExpect(jsonPath("$.data.username").value("updatedUsername"))
          .andExpect(jsonPath("$.data.email")
              .value("updatedEmail@example.com"));

      assertThat(logCaptor.getInfoLogs())
          .contains("Endpoint '/users/{id}' received PUT request with id = 2");
    }
  }

  /**
   * Verifies that an invalid UserUpdateRequestDTO returns an error.
   */
  @Test
  @WithMockUser
  void givenInvalidUserUpdateRequestDtoWhenUpdateUserThenReturnBadRequest() throws Exception {
    requestDto.setUsername("in");
    String invalidUsernameJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(put("/users/2")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidUsernameJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));

    requestDto.setUsername("updatedUsername");
    requestDto.setEmail("updatedEmailexample.com");
    String invalidEmailJson = objectMapper.writeValueAsString(requestDto);

    mockMvc.perform(put("/users/2")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidEmailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  /**
   * Verifies that an invalid user id returns an error.
   */
  @Test
  @WithMockUser
  void givenInvalidUserIdWhenUpdateUserThenReturnBadRequest() throws Exception {
    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserController.class)) {
      mockMvc.perform(put("/users/0")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(validJson))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("error"))
          .andExpect(jsonPath("$.message").value("User id must be greater than zero"));

      assertThat(logCaptor.getErrorLogs())
          .contains("Update user request attempted to get user by id with a non positive number");
    }
  }

  /**
   * Verifies that when the service returns an empty {@link Optional} then the response is an error.
   */
  @Test
  @WithMockUser
  void givenEmptyOptionalFromServiceWhenUpdateUserThenReturnNotFound() throws Exception {
    reset(logisticsUserService);
    when(logisticsUserService.updateUser(testUserId, requestDto)).thenReturn(Optional.empty());
    mockMvc.perform(put("/users/2")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validJson))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("User with id 2 does not exist"));
  }
}
