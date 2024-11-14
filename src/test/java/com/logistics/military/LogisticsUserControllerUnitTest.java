package com.logistics.military;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.military.controller.LogisticsUserController;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.service.LogisticsUserService;
import java.time.LocalDateTime;
import nl.altindag.log.LogCaptor;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit test class for the {@link LogisticsUserController}.
 *
 * <p>This class is designed to test the behavior of the UserController's endpoints. It utilizes
 * Spring's WebMvcTest support to create a mock web environment for testing, allowing for the
 * simulation of HTTP requests without needing to start a full server.
 * </p>
 *
 * <p>The class includes mocks for the UserService, PasswordEncoder, and UserRepository to isolate
 * the controller's behavior and avoid any dependencies on external systems such as databases or
 * external services. Each test method aims to validate the correctness of the controller's response
 * and functionality, ensuring that the appropriate HTTP status codes and response data are returned
 * when interacting with the /users endpoint.
 * </p>
 *
 * <p>{@code @ActiveProfiles("test")} activates the {@link ActiveProfiles} "test" profile,
 * loading properties from the application-test.properties file.
 * </p>
 *
 * <p>{@code @WebMvcTest(UserController.class)} uses {@link WebMvcTest} to load only the beans
 * relevant to the web layer.
 * </p>
 */
@ActiveProfiles("test")
@WebMvcTest(LogisticsUserController.class)
class LogisticsUserControllerUnitTest {

  private final MockMvc mockMvc; // MockMvc for testing the controller's endpoints

  // Using SpyBean to be able to test the logging.
  @SpyBean
  private final LogisticsUserService logisticsUserService;

  @MockBean
  private PasswordEncoder passwordEncoder; // Mocked PasswordEncoder dependency for UserService

  // Mocked UserRepository dependency for UserService
  @MockBean
  private LogisticsUserRepository logisticsUserRepository;

  // Define log captures for the classes that need logs captured.
  private LogCaptor serviceLogCaptor;
  private LogCaptor controllerLogCaptor;

  @Autowired
  LogisticsUserControllerUnitTest(LogisticsUserService service, MockMvc mockMvc) {
    this.logisticsUserService = service;
    this.mockMvc = mockMvc;
  }

  // Reinitialize LogCaptor before each test
  @BeforeEach
  void beforeEach() {
    serviceLogCaptor = LogCaptor.forClass(LogisticsUserService.class);
    controllerLogCaptor = LogCaptor.forClass(LogisticsUserController.class);
  }

  /**
   * Test the createUser endpoint to ensure it returns a created user. This test verifies that when
   * a valid user is posted to the /users endpoint, the response is a 201 Created status, and the
   * user data is returned correctly.
   */
  @Test
  void createUserShouldReturnCreatedUser() throws Exception {
    // Given
    LogisticsUser createdUser = new LogisticsUser(
        1L,
        "testUsername",
        "encodedPassword",
        "test@example.com",
        LocalDateTime.now()
    );
    doReturn(createdUser).when(logisticsUserService).addUser(any(LogisticsUser.class));
    // Given
    String json = "{\"username\":\"testUsername\", \"password\":\"testPassword\","
        + " \"email\":\"test@example.com\"}";

    // When & Then
    mockMvc.perform(post("/users")
        .with(csrf()) // add csrf token
        .with(user("testUser").password("password").roles("USER")) // add authentication details
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.username").value("testUsername"))
        .andExpect(jsonPath("$.user.email").value("test@example.com"));
  }

  /**
   * Test the createUser endpoint to ensure it returns an error. This test verifies that when
   * an invalid user is posted to the /users endpoint, the response is a 400 Bad Request status,
   * and an error is returned.
   */
  @Test
  void createUserWithInvalidIdShouldReturnError() throws Exception {
    // Given
    String json = "{\"userId\":\"1\", \"username\":\"testUser\", \"password\":\"testPassword\","
        + " \"email\":\"test@example.com\"}";

    // When & Then
    mockMvc.perform(post("/users")
        .with(csrf()) // add csrf token
        .with(user("testUser").password("password").roles("USER")) // add authentication details
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.user").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.error").value("Error during user creation"));

    // Test a log at warn level was logged by the UserService addUser method.
    assertThat(serviceLogCaptor.getWarnLogs()).isNotEmpty();
    assertThat(serviceLogCaptor.getWarnLogs().getFirst())
        .contains("Attempted creation of user with non-null id! Request Id: 1");

    // Test a log at debug level was logged by the UserController's post endpoint.
    assertThat(controllerLogCaptor.getDebugLogs()).isNotEmpty();
    assertThat(controllerLogCaptor.getDebugLogs().getFirst())
        .contains("User creation failed: User ID must be null when creating a new user.");
  }
}