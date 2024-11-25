package com.logistics.military;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.military.repository.LogisticsUserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for the UserController. This test verifies that the user creation endpoint
 * behaves as expected and that the user is saved correctly in the database.
 *
 * <p>{@code @SpringBootTest} loads the application context to be used in tests.
 * </p>
 *
 * <p>{@code @ActiveProfiles("test")} activates the "test" profile, loading properties from
 * the application-test.properties file.
 * </p>
 *
 * <p>{@code @AutoConfigureMockMvc} automatically configures the {@link MockMvc} instance
 * for use in testing the controller.
 * </p>
 *
 * <p>{@code @Transactional} ensures that each test method rolls back any changes made
 * during the test, helping to reset the application and database state and isolating
 * the environment for each test.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class LogisticsUserControllerIntegrationTest {

  // MockMvc for testing the controller's endpoints
  private final MockMvc mockMvc;

  // Mocked UserRepository for database interactions
  private final LogisticsUserRepository logisticsUserRepository;

  @Autowired
  public LogisticsUserControllerIntegrationTest(
      MockMvc mockMvc,
      LogisticsUserRepository logisticsUserRepository) {
    this.mockMvc = mockMvc;
    this.logisticsUserRepository = logisticsUserRepository;
  }

  /**
   * Test the createUser endpoint to ensure it returns a created user.
   *
   * <p>This test verifies that when a valid user is posted to the /users endpoint,
   * a returned response of 201 Created status, and the user is correctly saved in the database.
   */
  @Test
  void createUserShouldReturnCreatedUser() throws Exception {
    // Given
    String json = "{\"username\":\"testUser\", \"password\":\"testPassword\","
        + " \"email\":\"test@example.com\"}";

    // When & Then
    mockMvc.perform(post("/users")
        .with(csrf()) // add csrf token
        .with(user("testUser").password("password").roles("USER")) // add authentication details
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.username").value("testUser"))
        .andExpect(jsonPath("$.user.email").value("test@example.com"));

    // Assert that the user is actually saved in the database after the request
    assertTrue(logisticsUserRepository.findByUsername("testUser").isPresent());
  }

  /**
   * Test the createUser endpoint to ensure it returns an error and a database record is not stored.
   *
   * <p>This test verifies that when an invalid user is posted to the /users endpoint,
   * a returned response of 400 Bad Request status, and the user is not created in the database.
   */
  @Test
  void createUserWithInvalidIdShouldNotRecordToDatabase() throws Exception {
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

    // Assert that the user is not saved in the database after the request
    assertFalse(logisticsUserRepository.findByUsername("testUser").isPresent());
  }
}