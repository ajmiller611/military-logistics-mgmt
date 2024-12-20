package com.logistics.military.service;

import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.dto.UserUpdateRequestDto;
import com.logistics.military.exception.RoleNotFoundException;
import com.logistics.military.exception.UnauthorizedOperationException;
import com.logistics.military.exception.UserAlreadyExistsException;
import com.logistics.military.exception.UserCreationException;
import com.logistics.military.exception.UserNotFoundException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing and processing user-related operations,
 * including user registration, password encoding, role assignment, and user retrieval.
 *
 * <p>This service encapsulates the core business logic for user management,
 * ensuring secure and efficient handling of user data. It interacts with the
 * persistence layer through {@link LogisticsUserRepository} and integrates with
 * Spring Security for authentication and role-based access control.</p>
 */
@Service
@RequiredArgsConstructor
public class LogisticsUserService implements UserDetailsService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final LogisticsUserRepository logisticsUserRepository;
  private final Clock clock;

  private static final String ROLE_NAME_ADMIN = "ADMIN";
  private static final String NONEXISTENT_USER_ID_ERROR_MESSAGE = "User with id %d does not exist";

  /**
   * Registers a new user by validating the input data, encoding the password,
   * assigning a default role, and saving the user information to the database.
   *
   * <p>This method ensures secure handling of user data, including password encryption
   * and role assignment. It validates that the user is new, checks required fields for
   * null or empty values.
   * </p>
   *
   * @param userRequestDto the {@link UserRequestDto} containing the user's registration data.
   * @return a {@link LogisticsUserDto} object containing the registered user's details.
   * @throws IllegalArgumentException if required fields are empty or null.
   * @throws UserAlreadyExistsException if the username already exists in the database.
   * @throws IllegalStateException if the "USER" role is missing in the database.
   * @throws RuntimeException if an unexpected error occurs during user creation.
   */
  public LogisticsUserDto createAndSaveUser(UserRequestDto userRequestDto) {
    logger.info("Create user request with DTO: {}",  userRequestDto);

    // Validate user data for essential fields before processing.
    if (userRequestDto.getUsername() == null || userRequestDto.getUsername().isEmpty()) {
      throw new IllegalArgumentException("Username must not be empty.");
    }
    if (userRequestDto.getPassword() == null || userRequestDto.getPassword().isEmpty()) {
      throw new IllegalArgumentException("Password must not be empty.");
    }
    if (userRequestDto.getEmail() == null || userRequestDto.getEmail().isEmpty()) {
      throw new IllegalArgumentException("Email must not be empty.");
    }

    // Check for an existing user in the database.
    if (logisticsUserRepository.findByUsername(userRequestDto.getUsername()).isPresent()) {
      throw new UserAlreadyExistsException("User with username " + userRequestDto.getUsername()
                                           + " already exists.");
    }

    // Prepare user for registration by setting required fields and encoding the password.
    LogisticsUser user = new LogisticsUser();
    user.setUsername(userRequestDto.getUsername());
    user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
    user.setEmail(userRequestDto.getEmail());
    user.setCreatedAt(LocalDateTime.now(clock)); // Timestamp for auditing user creation.

    // Attempt to retrieve and assign the default role. Throws an error if the role is unavailable.
    Role userRole = roleRepository.findByAuthority("USER")
        .orElseThrow(() -> new IllegalStateException("USER role not found"));

    Set<Role> authorities = new HashSet<>();
    authorities.add(userRole);
    user.setAuthorities(authorities);

    // Save the user in the database to generate userId.
    try {
      user = logisticsUserRepository.save(user);
    } catch (DataAccessException e) {
      throw new UserCreationException("An error occurred while saving the user to the database", e);
    } catch (Exception e) {
      throw new UserCreationException(
          "An unexpected error occurred while saving the user to the database", e);
    }

    logger.info("LogisticsUser created: {}", user);
    return mapToUserDto(user);
  }

  /**
   * Retrieves a paginated list of users, excluding any users with the "ADMIN" role.
   *
   * <p>This method queries the database for all users without the "ADMIN" role before mapping
   * the remaining users to {@link UserResponseDto} objects.
   * </p>
   *
   * @param page the page number to retrieve.
   * @param size the number of users per page.
   * @return a {@link Page} containing {@link UserResponseDto} objects for the requested page,
   *         excluding users with the "ADMIN" role.
   */
  public Page<UserResponseDto> getUsers(int page, int size) {
    // Set the paging restrictions for the pageable object
    Pageable pageable = PageRequest.of(page, size);
    Role adminRole = roleRepository.findByAuthority(ROLE_NAME_ADMIN).orElseThrow(
        () -> new RoleNotFoundException("Role 'ADMIN' not found"));
    Page<LogisticsUser> usersPage =
        logisticsUserRepository.findAllWithoutRole(pageable, adminRole);

    // Convert the users to response DTOs
    List<UserResponseDto> userResponseDtos = usersPage.stream()
        .map(this::mapToUserResponseDto)
        .toList();

    return new PageImpl<>(userResponseDtos, pageable, userResponseDtos.size());
  }

  /**
   * Retrieves a user with the specified ID, excluding users with the "ADMIN" role.
   *
   * <p>This method queries the database for a user with the given ID. If the user has an
   * "ADMIN" role, the method returns an empty {@link Optional}. Non-existent user IDs
   * also result in an empty {@link Optional}.
   * </p>
   *
   * @param id the ID of the user to retrieve
   * @return an {@link Optional} containing the {@link LogisticsUser} if a user with the specified
   *         ID exists and does not have the "ADMIN" role, or an empty {@link Optional} otherwise.
   */
  public UserResponseDto getUserById(Long id) {
    LogisticsUser user = logisticsUserRepository.findById(id).orElseThrow(
        () -> new UserNotFoundException(
            String.format(NONEXISTENT_USER_ID_ERROR_MESSAGE, id), "getUserById"));

    if (user.hasRole(ROLE_NAME_ADMIN)) {
      throw new UnauthorizedOperationException(
          String.format("Unauthorized user cannot update admin user with id %d", id), id);
    }

    return new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }

  /**
   * Updates the details of an existing user based on the provided ID and update request.
   *
   * @param id the ID of the user to be updated
   * @param requestDto the {@link UserUpdateRequestDto} containing the new data for the user
   * @return a {@link UserResponseDto} containing the updated user's details
   */
  public UserResponseDto updateUser(Long id, UserUpdateRequestDto requestDto) {
    LogisticsUser user = logisticsUserRepository.findById(id).orElseThrow(
        () -> new UserNotFoundException(
            String.format(NONEXISTENT_USER_ID_ERROR_MESSAGE, id), "updateUser"));

    if (user.hasRole(ROLE_NAME_ADMIN)) {
      throw new UnauthorizedOperationException(
          String.format("Unauthorized user cannot update admin user with id %d", id), id);
    }

    user.setUsername(requestDto.getUsername());
    user.setEmail(requestDto.getEmail());
    user = logisticsUserRepository.save(user);

    return new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }

  /**
   * Deletes an existing user based on the provided id.
   *
   * <p>The method throws a {@link UserNotFoundException} if the id provided is an admin user.
   * Other exceptions are thrown for a user not existing and database-related exceptions for
   * the global exception handler to process.</p>
   *
   * @param id the id of the user to be deleted
   * @throws UserNotFoundException if the provided id does not exist in the database
   * @throws UnauthorizedOperationException if the user is an admin
   */
  public void deleteUser(Long id) {
    LogisticsUser user = logisticsUserRepository.findById(id).orElseThrow(
        () -> new UserNotFoundException(
            String.format(NONEXISTENT_USER_ID_ERROR_MESSAGE, id), "deleteUser"));

    if (user.hasRole(ROLE_NAME_ADMIN)) {
      throw new UnauthorizedOperationException(
          String.format("Unauthorized user cannot delete admin user with id %d", id), id);
    }

    logisticsUserRepository.deleteById(id);
  }

  /**
   * Loads a user by their username.
   *
   * <p>This method is required by {@link UserDetailsService} and is used by Spring Security
   * to authenticate a user. It retrieves a user based on their username and returns their
   * details, which include credentials and authorities.
   * </p>
   *
   * @param username the username of the user to load
   * @return the {@link UserDetails} containing user information for authentication
   * @throws UsernameNotFoundException if no user with the specified username is found
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return logisticsUserRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User is not valid"));
  }

  /**
   * Convert a {@link LogisticsUser} object to a {@link LogisticsUserDto} object.
   * This method also converts {@link org.springframework.security.core.GrantedAuthority} objects
   * into {@link Role} objects to reflect the user's roles.
   *
   * @param user a {@link LogisticsUser} object containing the user's data to be converted
   *             to a {@link LogisticsUserDto} object.
   * @return a {@link LogisticsUserDto} object containing the transformed user data.
   */
  public LogisticsUserDto mapToUserDto(LogisticsUser user) {
    Set<Role> roles = user.getAuthorities().stream()
        .map(authority -> {
          Role role = new Role();
          role.setAuthority(authority.getAuthority());
          return role;
        })
        .collect(Collectors.toSet());

    return new LogisticsUserDto(
        user.getUserId(),
        user.getUsername(),
        user.getEmail(),
        user.getCreatedAt(),
        roles
    );
  }

  /**
   * Convert a {@link LogisticsUser} object to a {@link UserResponseDto} object.
   *
   * @param user a {@link LogisticsUser} object containing the user's data to be converted to a
   *             {@link LogisticsUserDto} object.
   * @return a {@link UserResponseDto} object containing the transformed user data.
   */
  public UserResponseDto mapToUserResponseDto(LogisticsUser user) {
    return new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail());
  }
}