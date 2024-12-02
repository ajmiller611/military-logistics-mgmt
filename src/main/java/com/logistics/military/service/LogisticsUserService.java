package com.logistics.military.service;

import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.exception.UserAlreadyExistsException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    } catch (Exception e) {
      logger.error("Error occurred while saving user: {}", e.getMessage());
      throw new RuntimeException("An unexpected error occurred while creating the user.", e);
    }

    logger.info("LogisticsUser created: {}", user);
    return mapToUserDto(user);
  }

  /**
   * Retrieves a paginated list of users, excluding any users with the "ADMIN" role.
   *
   * <p>This method queries the database for all users and filters out users with the "ADMIN" role
   * before mapping the remaining users to {@link UserResponseDto} objects.
   * </p>
   *
   * <p>The filtering out "ADMIN" roles is for security purposes.</p>
   *
   * @param page the page number to retrieve.
   * @param size the number of users per page.
   * @return a {@link Page} containing {@link UserResponseDto} objects for the requested page,
   *         excluding users with the "ADMIN" role.
   */
  public Page<UserResponseDto> getUsers(int page, int size) {
    // Set the paging restrictions for the pageable object
    Pageable pageable = PageRequest.of(page, size);
    Page<LogisticsUser> usersPage = logisticsUserRepository.findAll(pageable);

    // Exclude admin users to protect admin user's details.
    List<LogisticsUser> filteredUsers = usersPage.getContent().stream().filter(
        user -> user.getAuthorities().stream().noneMatch(
            role -> role.getAuthority().equals("ADMIN")))
        .toList();

    // Convert the users to response DTOs
    List<UserResponseDto> userResponseDtos = filteredUsers.stream()
        .map(this::mapToUserResponseDto)
        .toList();

    return new PageImpl<>(userResponseDtos, pageable, filteredUsers.size());
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