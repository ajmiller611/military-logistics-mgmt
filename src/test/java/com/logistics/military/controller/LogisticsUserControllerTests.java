package com.logistics.military.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.military.config.AppConfig;
import com.logistics.military.config.SecurityConfig;
import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.LogisticsUserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link LogisticsUserController} class.
 *
 * <p>This test class verifies the functionality of the {@code LogisticsUserController}'s endpoints,
 * focusing on ensuring correct behavior of security configurations, request mappings,
 * response handling, and role-based access control.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Role-Based Access Control:</strong> Ensures that users with appropriate roles
 *     (e.g., "USER", "ADMIN") can access specific endpoints, while others are denied.
 *   </li>
 *   <li>
 *     <strong>Parameter Handling:</strong> Validates that default and custom parameters for
 *     paginated requests work as expected, and that invalid parameters result in proper
 *     error responses.
 *   </li>
 *   <li>
 *     <strong>Exception Handling:</strong> Confirms that application-level exceptions (e.g.,
 *     {@link DataAccessException} and {@link RuntimeException}) are correctly handled and logged,
 *     returning meaningful error responses to the client.
 *   </li>
 *   <li>
 *     <strong>Routing Validation:</strong> Verifies correct URI mappings for the "/users" endpoint
 *     to ensure the controller methods are invoked as expected.
 *   </li>
 *   <li>
 *     <strong>Response Structure:</strong> Tests the consistency of the response payload, including
 *     status, message, and data fields, aligning with the application's standardized
 *     response model.
 *   </li>
 * </ul>
 */
@WebMvcTest(LogisticsUserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
class LogisticsUserControllerTests {

  @InjectMocks private LogisticsUserController logisticsUserController;
  @Autowired private MockMvc mockMvc;
  @MockBean private LogisticsUserService logisticsUserService;
  @MockBean private TokenService tokenService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private LogisticsUserRepository logisticsUserRepository;

  /**
   * Sets up a mock response for the 'getUsers' method with default parameters (page = 0, size = 10)
   * for each test.
   */
  @BeforeEach
  void setUp() {
    Page<UserResponseDto> pagedUsers = createPagedUserResponseDtos(0, 10);
    when(logisticsUserService.getUsers(0, 10)).thenReturn(pagedUsers);
  }

  /**
   * Verifies that a user with the "USER" role can access the "/users" endpoint successfully.
   */
  @Test
  @WithMockUser(username = "testUser") // roles param default value is "USER"
  void givenUserWithRoleUserWhenGetUsersThenReturnsSuccessStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  /**
   * Verifies that a user with the "ADMIN" role can access the "/users" endpoint successfully.
   */
  @Test
  @WithMockUser(username = "authorizedUser", roles = {"ADMIN"})
  void givenUserWithRoleAdminWhenGetUsersThenReturnsSuccessStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  /**
   * Verifies that a user without valid roles cannot access the "/users" endpoint.
   */
  @Test
  @WithMockUser(username = "authorizedUser", roles = {"GUEST"})
  void givenNoRoleWhenGetUsersThenRespondsWithForbiddenStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isForbidden());
  }

  /**
   * Verifies that a user without a role cannot access the "/users" endpoint.
   */
  @Test
  @WithMockUser(username = "unauthorizedUser", roles = {})
  void givenEmptyRoleWhenGetUsersThenRespondsWithForbiddenStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isForbidden());
  }

  /**
   * Ensures an unauthenticated request to "/users" responds with "unauthorized".
   */
  @Test
  @WithAnonymousUser
  void givenUnauthenticatedWhenGetUsersThenRespondsWithUnauthorizedStatus() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Verifies that "/users" and "/users/" routes map to the correct method in the controller.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenUsersUrisWhenGetThenRoutesToGetUsersMethod() throws Exception {
    mockMvc.perform(get("/users")
            .param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(handler().methodName("getUsers"));

    mockMvc.perform(get("/users/")
            .param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(handler().methodName("getUsers"));
  }

  /**
   * Ensures default pagination parameters are used when no parameters are provided.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenNoParamsWhenGetUsersThenReturnsSuccessWithDefaults() throws Exception {
    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // Verify default values were used
    verify(logisticsUserService, times(1)).getUsers(0, 10);
  }

  /**
   * Verifies that custom pagination parameters are passed to the service.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenCustomParamsWhenGetUsersThenCallsServiceWithParams() throws Exception {
    Page<UserResponseDto> customPagedUsers = createPagedUserResponseDtos(1, 5);
    when(logisticsUserService.getUsers(1, 5)).thenReturn(customPagedUsers);

    mockMvc.perform(get("/users")
            .param("page", "1")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));

    // Verify getUsers method was called once with the custom values
    verify(logisticsUserService, times(1)).getUsers(1, 5);
  }

  /**
   * Verifies that invalid pagination parameters result in a "bad request" error.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenInvalidParametersWhenGetUsersThenRespondsWithBadRequestAndErrorMessage()
      throws Exception {
    mockMvc.perform(get("/users")
            .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message")
            .value("Page number must not be negative"));

    mockMvc.perform(get("/users")
            .param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message")
            .value("Size must be a positive number"));
  }

  /**
   * Verifies that the "/users" endpoint excludes admin users in the response.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenValidRequestWhenGetUsersThenReturnsSuccessAndExpectedData() throws Exception {
    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserController.class)) {
      mockMvc.perform(get("/users"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("success"))
          .andExpect(jsonPath("$.message")
              .value("Users retrieved successfully"))
          .andExpect(jsonPath("$.data.data[0].userId").value(2L))
          .andExpect(jsonPath("$.data.data[0].username").value("testUser"))
          .andExpect(jsonPath("$.data.data[0].email").value("test@example.com"))
          .andExpect(jsonPath("$.data.data[1].userId").value(3L))
          .andExpect(jsonPath("$.data.data[1].username").value("testUser1"))
          .andExpect(jsonPath("$.data.data[1].email").value("test1@example.com"))
          .andExpect(jsonPath("$.data.data[0].username", not("admin")))
          .andExpect(jsonPath("$.data.data[1].username", not("admin")))
          .andExpect(jsonPath("$.data.currentPage").value(0))
          .andExpect(jsonPath("$.data.totalPages").value(1))
          .andExpect(jsonPath("$.data.totalItems").value(2));

      verify(logisticsUserService, times(1)).getUsers(anyInt(), anyInt());

      assertThat(logCaptor.getInfoLogs())
          .anyMatch(log ->
              log.contains("Retrieved users for page"));
    }
  }

  /**
   * Verifies the correct handling of empty data in the response.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenEmptyUserListWhenGetUsersThenReturnsSuccessWithEmptyData() throws Exception {
    Page<UserResponseDto> emptyPage =
        new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
    when(logisticsUserService.getUsers(0, 10)).thenReturn(emptyPage);

    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data.data").isEmpty())
        .andExpect(jsonPath("$.data.currentPage").value(0))
        .andExpect(jsonPath("$.data.totalPages").value(0))
        .andExpect(jsonPath("$.data.totalItems").value(0));

    verify(logisticsUserService, times(1)).getUsers(anyInt(), anyInt());
  }

  /**
   * Ensures database errors are logged and handled gracefully.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenDatabaseErrorWhenGetUsersThenReturnsInternalServerError() throws Exception {
    when(logisticsUserService.getUsers(0, 10))
        .thenThrow(
            new DataAccessException("An unexpected error occurred while fetching user data"){});

    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserController.class)) {
      mockMvc.perform(get("/users"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.status").value("error"))
          .andExpect(jsonPath("$.message")
              .value("An unexpected error occurred while fetching user data"));

      assertThat(logCaptor.getErrorLogs())
          .anyMatch(log ->
              log.contains("Database error occurred while fetching users:"));
    }
  }

  /**
   * Ensures generic errors are logged and handled gracefully.
   */
  @Test
  @WithMockUser(username = "testUser")
  void givenRuntimeExceptionWhenGetUsersThenReturnsInternalServerError() throws Exception {
    when(logisticsUserService.getUsers(0, 10))
        .thenThrow(new RuntimeException("An unexpected error occurred"));

    try (LogCaptor logCaptor = LogCaptor.forClass(LogisticsUserController.class)) {
      mockMvc.perform(get("/users"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.status").value("error"))
          .andExpect(jsonPath("$.message")
              .value("An unexpected error occurred"));

      assertThat(logCaptor.getErrorLogs())
          .anyMatch(log ->
              log.contains("Unexpected error occurred:"));
    }
  }

  /**
   * Creates a paginated response of {@link UserResponseDto} objects for testing purposes.
   *
   * @param page the page number to simulate (0-based index)
   * @param size the number of users per page
   * @return a {@link Page} containing the {@link UserResponseDto} objects for the specified page
   *     and size
   */
  private Page<UserResponseDto> createPagedUserResponseDtos(int page, int size) {
    List<UserResponseDto> users = Arrays.asList(
        new UserResponseDto(2L, "testUser", "test@example.com"),
        new UserResponseDto(3L, "testUser1", "test1@example.com")
    );
    return new PageImpl<>(users, PageRequest.of(page, size), users.size());
  }
}
