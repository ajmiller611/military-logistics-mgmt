package com.logistics.military.service;

import com.logistics.military.model.LogisticsUser;
import com.logistics.military.repository.LogisticsUserRepository;
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
 * data access layer through the {@link LogisticsUserRepository}. It ensures that user data is
 * processed correctly, including password encoding, before being persisted in the
 * database.</p>
 */
@Service
public class LogisticsUserService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PasswordEncoder passwordEncoder;
  private final LogisticsUserRepository logisticsUserRepository;

  /**
   * Constructor for {@link LogisticsUserService}.
   *
   * <p>Uses constructor-based dependency injection to ensure required dependencies
   * ({@link PasswordEncoder} and {@link LogisticsUserRepository}) are provided,
   * promoting testability and loose coupling when the {@link LogisticsUserService} is instantiated.
   * </p>
   *
   * @param passwordEncoder the {@link PasswordEncoder} to use for encoding user passwords.
   * @param logisticsUserRepository the {@link LogisticsUserRepository} to interact with user
   *                                data in the database.
   */
  public LogisticsUserService(
      PasswordEncoder passwordEncoder,
      LogisticsUserRepository logisticsUserRepository) {
    this.passwordEncoder = passwordEncoder;
    this.logisticsUserRepository = logisticsUserRepository;
  }

  /**
   * Encodes the password of a new user and saves the user to the repository.
   *
   * @param newUser the {@link LogisticsUser} object containing the user's details.
   * @return the saved {@link LogisticsUser}) object with an encoded password.
   */
  public LogisticsUser addUser(LogisticsUser newUser) {
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
    return logisticsUserRepository.save(newUser);
  }
}