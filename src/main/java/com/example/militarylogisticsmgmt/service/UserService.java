package com.example.militarylogisticsmgmt.service;

import com.example.militarylogisticsmgmt.model.User;
import com.example.militarylogisticsmgmt.repository.UserRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Service class responsible for managing user-related operations, including creating,
 * saving, and manipulating user data in the application.
 *
 * <p>This class encapsulates the business logic related to users and interacts with the
 * data access layer through the {@link UserRepository}. It ensures that user data is
 * processed correctly, including password encoding, before being persisted in the
 * database.</p>
 */
@Service
public class UserService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  /**
   * Constructor for UserService.
   *
   * <p>Uses constructor-based dependency injection to ensure required dependencies (PasswordEncoder
   * and UserRepository) are provided, promoting testability and loose coupling when the UserService
   * is instantiated.
   * </p>
   *
   * @param passwordEncoder the {@link PasswordEncoder} to use for encoding user passwords.
   * @param userRepository  the {@link UserRepository} to interact with user data in the database.
   */
  public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
  }

  /**
   * Encodes the password of a new user and saves the user to the repository.
   *
   * @param newUser the {@link User} object containing the user's details.
   * @return the saved {@link User}) object with an encoded password.
   */
  public User addUser(User newUser) {
    /*
     * Ensure UserId is null for JPA to assign a value when adding a new user to the database.
     * A non-null userId would attempt to modify an existing record.
     */
    if (newUser.getUserId() != null) {
      /*
       * Logs an attempt to create a user with a non-null ID, which may indicate potential misuse
       * or malicious activity. This information is recorded for auditing purposes.
       */
      logger.warn("Attempted creation of user with non-null id! Request Id: {}",
          newUser.getUserId());
      throw new IllegalArgumentException("User ID must be null when creating a new user.");
    }

    // Set the created timestamp to now
    newUser.setCreatedAt(LocalDateTime.now());
    // Encode the password before saving the user details to the repository.
    newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
    return userRepository.save(newUser);
  }
}