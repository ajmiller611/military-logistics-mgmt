package com.logistics.military.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logistics.military.config.AppConfig;
import com.logistics.military.config.SecurityConfig;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.LogisticsUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the {@link LogisticsUserController} class.
 *
 * <p>This test class is designed to verify the functionality of endpoints in the
 * {@code LogisticsUserController} with a focus on ensuring the security configurations,
 * endpoint mappings, and expected responses are correct.
 * It includes tests for role-based access control to ensure that only users with appropriate
 * roles can access the protected endpoints.
 * </p>
 *
 * <h2>Current Tests</h2>
 * <ul>
 *   <li>
 *     {@code givenAuthenticatedUserWithRoleUserWhenUserRoleAccessThenReturnMessage} - Verifies that
 *     an authenticated user with the "USER" role can access the "/users/" endpoint, expecting a
 *     200 OK status and the string "User access level" in the response body.
 *   </li>
 *   <li>
 *     {@code givenUserWithAdminRoleWhenUserRoleAccessThenReturnMessage} - Verifies that
 *     an authenticated user with the "ADMIN" role can access the "/users/" endpoint, expecting
 *     a 200 OK status and the string "User access level" in the response body.
 *   </li>
 *   <li>
 *     {@code givenNoUserRoleWhenUserRoleAccessThenReturnForbidden} - Verifies that a user
 *     without the "USER" role is denied access to the "/users/" endpoint, expecting a 403 Forbidden
 *     status.
 *   </li>
 *   <li>
 *     {@code givenEmptyRolesWhenUserRoleAccessThenReturnForbidden} - Verifies that a user with
 *     no roles at all is denied access to the "/users/" endpoint, expecting a 403 Forbidden status.
 *   </li>
 *   <li>
 *     {@code givenUnauthenticatedRequestWhenUserRoleAccessThenReturnUnauthorized} - Verifies that
 *     an unauthenticated user is denied access to the "/users/" endpoint, expecting a
 *     401 Unauthorized status.
 *   </li>
 * </ul>
 */
@WebMvcTest(LogisticsUserController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
public class LogisticsUserControllerTests {

  @InjectMocks private LogisticsUserController logisticsUserController;
  @Autowired private MockMvc mockMvc;
  @MockBean private LogisticsUserService logisticsUserService;
  @MockBean private TokenService tokenService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private LogisticsUserRepository logisticsUserRepository;

  /**
   * Tests the "/users/" endpoint for authenticated users with the "USER" role.
   */
  @Test
  @WithMockUser(username = "testUser", roles = {"USER"})
  void givenAuthenticatedUserWithRoleUserWhenUserRoleAccessThenReturnMessage() throws Exception {
    mockMvc.perform(get("/users/"))
        .andExpect(status().isOk())
        .andExpect(content().string("User access level"));
  }

  /**
   * Tests the "/users/" endpoint for users with "ADMIN" roles.
   */
  @Test
  @WithMockUser(username = "unauthorizedUser", roles = {"ADMIN"})
  void givenUserWithAdminRoleWhenUserRoleAccessThenReturnMessage() throws Exception {
    mockMvc.perform(get("/users/"))
        .andExpect(status().isOk())
        .andExpect(content().string("User access level"));
  }

  /**
   * Tests the "/users/" endpoint for users without the "USER" role present.
   */
  @Test
  @WithMockUser(username = "unauthorizedUser", roles = {"GUEST"})
  void givenNoUserRoleWhenUserRoleAccessThenReturnForbidden() throws Exception {
    mockMvc.perform(get("/users/"))
        .andExpect(status().isForbidden());
  }

  /**
   * Tests the "/users/" endpoint for users with empty roles.
   */
  @Test
  @WithMockUser(username = "unauthorizedUser", roles = {})
  void givenEmptyRolesWhenUserRoleAccessThenReturnForbidden() throws Exception {
    mockMvc.perform(get("/users/"))
        .andExpect(status().isForbidden());
  }

  /**
   * Tests the "/users/" endpoint for when an unauthenticated request is received.
   */
  @Test
  @WithAnonymousUser
  void givenUnauthenticatedRequestWhenUserRoleAccessThenReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/users/"))
        .andExpect(status().isUnauthorized());
  }
}
