package com.example.military_logistics_mgmt.service;

import com.example.military_logistics_mgmt.model.User;
import com.example.military_logistics_mgmt.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/*
 * Service class for managing user operations such as creating and saving users.
 */
@Service
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * Constructor for UserService.
     * <p>
     * Uses constructor-based dependency injection to ensure required dependencies
     * (PasswordEncoder and UserRepository) are provided, promoting testability and loose coupling
     * when the UserService is instantiated.
     * </p>
     *
     * @param passwordEncoder the {@link PasswordEncoder} to use for encoding user passwords.
     * @param userRepository  the repository to interact with user data in the database.
     */
    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    /**
     * Encodes the password of a new user and saves the user to the repository.
     *
     * @param newUser the user object containing the user's details.
     * @return the saved User object with an encoded password.
     */
    public User addUser(User newUser) {

        /*
         * Ensure UserId is null for JPA to assign a value when adding a new user to the database.
         * A non-null userId could indicate an attempt to modify an existing record.
         */
        if (newUser.getUserId() != null) {
            /*
             * Logs an attempt to create a user with a non-null ID, which may indicate potential misuse.
             * This information is recorded for auditing purposes.
             */
            logger.warn("Attempted creation of user with non-null id! Request Id: {}", newUser.getUserId());
            throw new IllegalArgumentException("User ID must be null when creating a new user.");
        }

        // Set the created timestamp to now
        newUser.setCreatedAt(LocalDateTime.now());
        // Encode the password before saving the user details to the repository.
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        return userRepository.save(newUser);
    }
}
