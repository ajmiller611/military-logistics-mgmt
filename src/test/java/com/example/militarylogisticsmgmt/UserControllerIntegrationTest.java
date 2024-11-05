package com.example.militarylogisticsmgmt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.militarylogisticsmgmt.repository.UserRepository;
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
class UserControllerIntegrationTest {

  private final MockMvc mockMvc; // MockMvc for testing the controller's endpoints

  private final UserRepository userRepository; // Mocked UserRepository for database interactions

  @Autowired
  public UserControllerIntegrationTest(MockMvc mockMvc, UserRepository userRepository) {
    this.mockMvc = mockMvc;
    this.userRepository = userRepository;
  }

  /**
   * Test the createUser endpoint to ensure it returns a created user.
   *
   * <p>This test verifies that when a valid user is posted to the /users endpoint,
   * a returned response of 201 Created status, and the user is correctly saved in the H2 database.
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

    // Assert that the user is actually saved in the H2 database after the request
    assertTrue(userRepository.findByUsername("testUser").isPresent());
  }
}