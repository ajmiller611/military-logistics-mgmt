package com.example.military_logistics_mgmt;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.example.military_logistics_mgmt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for the UserController.
 * This test verifies that the user creation endpoint behaves as expected
 * and that the user is saved correctly in the database.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    /**
     * Test the createUser endpoint to ensure it returns a created user.
     *
     * This test verifies that when a valid user is posted to the /users endpoint,
     * the response is a 201 Created status, and the user is correctly saved
     * in the H2 database.
     */
    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        // Given
        String json = "{\"username\":\"testUser\", \"password\":\"testPassword\", \"email\":\"test@example.com\"}";

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