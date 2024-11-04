package com.example.military_logistics_mgmt.controller;

import com.example.military_logistics_mgmt.model.User;
import com.example.military_logistics_mgmt.response.UserResponse;
import com.example.military_logistics_mgmt.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling user-related HTTP requests.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserService userService;

    /*
     * Use constructor-based dependency injection to ensure the UserService is provided,
     * promoting testability and loose coupling when UserController is instantiated.
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles HTTP POST requests to create a new user in the system.
     *
     * @param newUser a {@link User} object containing the user details from the request body.
     * @return a {@link ResponseEntity} containing a {@link UserResponse} object with the created
     * user's details and a {@link HttpStatus#CREATED} status if successful, or a {@link UserResponse}
     * object with an error message {@link HttpStatus#BAD_REQUEST} if an {@link IllegalArgumentException}
     * is caught, indicating that an error occurred while attempting to add the user.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody User newUser) {
        try {
            // Pass the user to the service layer for addition to the database
            User createdUser = userService.addUser(newUser);

            // Log the new record for debugging and auditing purposes.
            logger.debug("User created: {}", createdUser);

            // Create response object with the created User
            UserResponse response = new UserResponse(createdUser, null);

            // Return a response with status 201 Created and the created user
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            // Log the user creation failure with detailed error message for debugging and auditing purposes.
            logger.warn("User creation failed: {}", e.getMessage());

            /*
             * Create a response object with a generic error message to prevent exposing internal logic
             * to potential attackers.
             */
            UserResponse response = new UserResponse(null, "Error during user creation");

            // Return a response with status 400 Bad Request and the generic error message
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}

