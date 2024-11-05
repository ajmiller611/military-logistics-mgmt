package com.example.militarylogisticsmgmt;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.militarylogisticsmgmt.controller.UserController;
import com.example.militarylogisticsmgmt.model.User;
import com.example.militarylogisticsmgmt.repository.UserRepository;
import com.example.militarylogisticsmgmt.service.UserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit test class for the {@link UserController}.
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
@WebMvcTest(UserController.class)
class UserControllerUnitTest {

  private final MockMvc mockMvc; // MockMvc for testing the controller's endpoints

  @MockBean
  private UserService userService; // Mocked UserService to isolate the controller's behavior

  @MockBean
  private PasswordEncoder passwordEncoder; // Mocked PasswordEncoder to handle password encoding

  @MockBean
  private UserRepository userRepository; // Mocked UserRepository to prevent database interactions

  @Autowired
  public UserControllerUnitTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  /**
   * Test the createUser endpoint to ensure it returns a created user. This test verifies that when
   * a valid user is posted to the /users endpoint, the response is a 201 Created status, and the
   * user data is returned correctly.
   */
  @Test
  void createUserShouldReturnCreatedUser() throws Exception {
    // Given
    User newUser = new User(
        null,
        "testUsername",
        "testPassword",
        "test@example.com",
        null
    );

    assertNull(newUser.getUserId(), "UserId should be null for a new user");

    User createdUser = new User(
        1L,
        "testUsername",
        "encodedPassword",
        "test@example.com",
        LocalDateTime.now()
    );

    when(userService.addUser(any(User.class))).thenReturn(createdUser);

    // When & Then
    mockMvc.perform(post("/users")
            .with(csrf()) // add csrf token
            .with(user("testUser").password("password").roles("USER")) // add authentication details
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"username\":\"testUsername\", \"password\":\"testPassword\","
                    + " \"email\":\"test@example.com\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.username").value("testUsername"))
        .andExpect(jsonPath("$.user.email").value("test@example.com"));
  }
}