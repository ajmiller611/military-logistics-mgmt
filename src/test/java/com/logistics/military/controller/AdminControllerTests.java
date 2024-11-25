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
 * Unit tests for the {@link AdminController} class.
 *
 * <p>This test class is designed to verify the functionality of endpoints in the
 * {@code AdminController} with a focus on ensuring the security configurations,
 * endpoint mappings, and expected responses are correct.
 * It includes tests for role-based access control to ensure that only users with appropriate
 * roles can access the protected endpoints.
 * </p>
 *
 * <h2>Current Tests</h2>
 * <ul>
 *   <li>
 *     {@code givenAuthenticatedUserWithRoleAdminWhenAdminRoleAccessThenReturnMessage} - Verifies
 *     that an authenticated user with the "ADMIN" role can access the "/admin/" endpoint,
 *     expecting a 200 OK status and the string "Admin access level" in the response body.
 *   </li>
 *   <li>
 *     {@code givenNoAdminRoleWhenAdminRoleAccessThenReturnForbidden} - Verifies that a user
 *     without the "ADMIN" role is denied access to the "/admin/" endpoint, expecting a
 *     403 Forbidden status.
 *   </li>
 *   <li>
 *     {@code givenEmptyRolesWhenAdminRoleAccessThenReturnForbidden} - Verifies that a user with
 *     no roles at all is denied access to the "/admin/" endpoint, expecting a 403 Forbidden status.
 *   </li>
 *   <li>
 *     {@code givenUnauthenticatedRequestWhenAdminRoleAccessThenReturnUnauthorized} - Verifies that
 *     an unauthenticated user is denied access to the "/admin/" endpoint, expecting a
 *     401 Unauthorized status.
 *   </li>
 * </ul>
 */
@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, AppConfig.class})
@ActiveProfiles("test")
public class AdminControllerTests {

  @InjectMocks private LogisticsUserController logisticsUserController;
  @Autowired private MockMvc mockMvc;
  @MockBean private LogisticsUserService logisticsUserService;
  @MockBean private TokenService tokenService;
  @MockBean private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private RoleRepository roleRepository;
  @MockBean private LogisticsUserRepository logisticsUserRepository;

  /**
   * Tests the "/admin/" endpoint for authenticated users with the "ADMIN" role.
   */
  @Test
  @WithMockUser(username = "testUser", roles = {"ADMIN"})
  void givenAuthenticatedUserWithRoleAdminWhenAdminRoleAccessThenReturnMessage() throws Exception {
    mockMvc.perform(get("/admin/"))
        .andExpect(status().isOk())
        .andExpect(content().string("Admin access level"));
  }

  /**
   * Tests the "/admin/" endpoint for users without the "ADMIN" role present.
   */
  @Test
  @WithMockUser(username = "unauthorizedUser", roles = {"USER"})
  void givenNoAdminRoleWhenAdminRoleAccessThenReturnForbidden() throws Exception {
    mockMvc.perform(get("/admin/"))
        .andExpect(status().isForbidden());
  }

  /**
   * Tests the "/admin/" endpoint for users with empty roles.
   */
  @Test
  @WithMockUser(username = "unauthorizedUser", roles = {})
  void givenEmptyRolesWhenAdminRoleAccessThenReturnForbidden() throws Exception {
    mockMvc.perform(get("/admin/"))
        .andExpect(status().isForbidden());
  }

  /**
   * Tests the "/admin/" endpoint for when an unauthenticated request is received.
   */
  @Test
  @WithAnonymousUser
  void givenUnauthenticatedRequestWhenAdminRoleAccessThenReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/admin/"))
        .andExpect(status().isUnauthorized());
  }
}
