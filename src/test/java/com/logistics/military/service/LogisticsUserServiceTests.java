package com.logistics.military.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class, designed to validate its core
 * functionalities, edge cases, and error handling mechanisms.
 *
 * <p>The tests cover operations such as:
 * <ul>
 *   <li>Mapping between domain models ({@link LogisticsUser}) and DTOs ({@link LogisticsUserDto},
 *       {@link UserResponseDto}).</li>
 *   <li>Creating and saving users, including validation of inputs, role assignments,
 *       and exception handling.</li>
 *   <li>Loading user details for authentication, ensuring the correct handling of existing
 *       and non-existing users.</li>
 *   <li>Pagination and filtering logic for retrieving user data, including exclusion
 *       of admin users from results.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceTests {

  @InjectMocks private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Clock clock;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

  UserRequestDto userRequestDto;
  Role userRole;
  LogisticsUser user;
  Pageable pageable;
  Page<LogisticsUser> pagedUsers;

  /**
   * Sets up test data for use before each test case.
   *
   * <p>This method includes details for a {@link UserRequestDto} and {@link LogisticsUser} objects.
   * This represents the objects needed to mock a user and pass user data in the system.
   * </p>
   */
  @BeforeEach
  void setUp() {
    userRequestDto = new UserRequestDto(
        "testUser",
        "password",
        "test@example.com"
    );

    userRole = new Role("USER");
    user = new LogisticsUser(
        2L,
        userRequestDto.getUsername(),
        userRequestDto.getPassword(),
        userRequestDto.getEmail(),
        fixedTimestamp,
        Set.of(userRole)
    );

    // Create a simple and common response for testing the getUsers method
    pageable = PageRequest.of(0, 10);
    pagedUsers = createPagedLogisticsUsers(0, 10, 2, false);
  }

  /**
   * Test case for the utility method of mapping a {@link LogisticsUser} to a
   * {@link LogisticsUserDto}. This test makes sure that the mapping is correctly done.
   */
  @Test
  void givenLogisticsUserWhenMapToUserDtoThenReturnLogisticsUserDto() {
    LogisticsUserDto dto = logisticsUserService.mapToUserDto(user);

    assertNotNull(dto);
    assertEquals(user.getUserId(), dto.getUserId());
    assertEquals(user.getUsername(), dto.getUsername());
    assertEquals(user.getEmail(), dto.getEmail());
    assertEquals(user.getCreatedAt(), dto.getCreatedAt());
    assertEquals(1, dto.getAuthorities().size());
    assertEquals("USER", dto.getAuthorities().iterator().next().getAuthority());
  }

  /**
   * Test case for when a valid request DTO is provided to the service. The service should return a
   * populated {@link LogisticsUserDto} with the newly created user's details.
   */
  @Test
  void givenValidUserRequestDtoWhenCreateAndSaveUserThenReturnLogisticsUserDto() {

    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority(userRole.getAuthority())).thenReturn(Optional.of(userRole));
    when(logisticsUserRepository.save(any(LogisticsUser.class))).thenReturn(user);

    LogisticsUserDto userDto = logisticsUserService.createAndSaveUser(userRequestDto);

    assertNotNull(userDto);
    assertEquals(user.getUserId(), userDto.getUserId());
    assertEquals(user.getUsername(), userDto.getUsername());
    assertEquals(user.getEmail(), userDto.getEmail());
    assertEquals(user.getCreatedAt(), userDto.getCreatedAt());
    assertEquals(user.getAuthorities(), userDto.getAuthorities());
  }

  /**
   * Test case for when an invalid username is in the {@link UserRequestDto}. This shouldn't happen
   * since the username should have gone through validation in the controller. This test verifies
   * the backup check in {@link LogisticsUserService} is working correctly.
   */
  @Test
  void givenInvalidUsernameWhenCreateAndSaveUserThenThrowIllegalArgumentException() {
    userRequestDto.setUsername(null);
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an null username");

    userRequestDto.setUsername("");
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an empty username");
  }

  /**
   * Test case for when an invalid password is in the {@link UserRequestDto}. This shouldn't happen
   * since the password should have gone through validation in the controller. This test verifies
   * the backup check in {@link LogisticsUserService} is working correctly.
   */
  @Test
  void givenInvalidPasswordWhenCreateAndSaveUserThenThrowIllegalArgumentException() {
    userRequestDto.setPassword(null);
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an null password");

    userRequestDto.setPassword("");
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an empty password");
  }

  /**
   * Test case for when an invalid email s in the {@link UserRequestDto}. This shouldn't happen
   * since the email should have gone through validation in the controller. This test verifies
   * the backup check in {@link LogisticsUserService} is working correctly.
   */
  @Test
  void givenInvalidEmailWhenCreateAndSaveUserThenThrowIllegalArgumentException() {
    userRequestDto.setEmail(null);
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an null email");

    userRequestDto.setEmail("");
    assertThrows(IllegalArgumentException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an empty email");
  }

  /**
   * Test case for when the username already exists in the database. The service should throw an
   * {@link UserAlreadyExistsException} when trying to create a user that already exists.
   */
  @Test
  void givenExistingUserWhenCreateAndSaveUserThenThrowUserAlreadyExistsException() {
    when(logisticsUserRepository.findByUsername(userRequestDto.getUsername()))
        .thenReturn(Optional.of(new LogisticsUser()));

    assertThrows(UserAlreadyExistsException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for an existing user");
  }

  /**
   * Test case for when the 'USER' role can not be found in the database. The service should throw
   * an {@link IllegalStateException} when the 'USER' role does not exist.
   */
  @Test
  void givenRoleNotFoundWhenCreateAndSaveUserThenThrowIllegalStateException() {
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception for role not found");
  }

  /**
   * Test case for when an error occurs when trying to save the user to the database. The service
   * should throw a {@link RuntimeException} when a database error occurs.
   */
  @Test
  void givenDatabaseErrorWhenCreateAndSaveUserThenThrowRuntimeException() {
    when(logisticsUserRepository.findByUsername(userRequestDto.getUsername()))
        .thenReturn(Optional.empty());
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    when(roleRepository.findByAuthority("USER")).thenReturn(Optional.of(userRole));
    when(logisticsUserRepository.save(any(LogisticsUser.class)))
        .thenThrow(new RuntimeException("Database save error"));

    assertThrows(RuntimeException.class,
        () -> logisticsUserService.createAndSaveUser(userRequestDto),
        "Expected createAndSaveUser to throw an exception when error saving"
        + " to the database occurs.");
  }

  /**
   * Test case for when a user exists and needs to be queried for authentication. The service should
   * return an {@link UserDetails} with the user's details found in the database.
   */
  @Test
  void givenUserExistsWhenLoadUserByUsernameThenReturnUserDetails() {
    String username = "testUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.of(user));

    UserDetails userDetails = logisticsUserService.loadUserByUsername(username);

    assertNotNull(userDetails);
    assertEquals(username, userDetails.getUsername());
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }

  /**
   * Test case for when a user does not exist but is queried for authentication. The service should
   * throw an {@link UsernameNotFoundException}.
   */
  @Test
  void givenUserDoesNotExistsWhenLoadUserByUsernameThenThrowUsernameNotFoundException() {
    String username = "invalidUser";
    when(logisticsUserRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class,
        () -> logisticsUserService.loadUserByUsername(username),
        "Expected loadUserByUsername to throw exception when user does not exist.");
    verify(logisticsUserRepository, times(1)).findByUsername(username);
  }

  /**
   * Ensures that {@link LogisticsUser} objects are converted to {@link UserResponseDto} objects.
   */
  @Test
  void givenLogisticsUserWhenMapToUserResponseDtoThenReturnUserResponseDto() {
    UserResponseDto responseDto = logisticsUserService.mapToUserResponseDto(user);

    assertNotNull(responseDto);
    assertEquals(user.getUserId(), responseDto.getUserId());
    assertEquals(user.getUsername(), responseDto.getUsername());
    assertEquals(user.getEmail(), responseDto.getEmail());
  }

  /**
   * Verifies that users are queried from the database and converted to DTOs correctly.
   */
  @Test
  void givenValidPageAndSizeWhenGetUsersThenReturnUserResponsePage() {
    when(logisticsUserRepository.findAll(pageable)).thenReturn(pagedUsers);

    Page<UserResponseDto> result = logisticsUserService.getUsers(0, 10);

    verify(logisticsUserRepository, times(1)).findAll(pageable);
    assertNotNull(result);
    assertEquals(pagedUsers.getNumber(), result.getNumber());
    assertEquals(pagedUsers.getTotalPages(), result.getTotalPages());
    assertEquals(pagedUsers.getTotalElements(), result.getTotalElements());
    assertEquals(pagedUsers.getContent().size(), result.getContent().size());
    assertIterableEquals(pagedUsers.getContent().stream()
        .map(logisticsUserService::mapToUserResponseDto)
        .toList(), result.getContent());
  }

  /**
   * Verifies that when no valid users are queried then an empty page is returned.
   */
  @Test
  void givenNoUsersWhenGetUsersThenReturnsEmptyPage() {
    Page<LogisticsUser> emptyPage =
        new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
    when(logisticsUserRepository.findAll(pageable)).thenReturn(emptyPage);

    Page<UserResponseDto> result = logisticsUserService.getUsers(0, 10);

    verify(logisticsUserRepository, times(1)).findAll(pageable);
    assertNotNull(result);
    assertTrue(result.isEmpty(), "Result should be an empty page");
    assertEquals(0, result.getNumber(), "Current page number should be 0");
    assertEquals(0, result.getTotalPages(), "Total pages should be 0");
    assertEquals(0, result.getTotalElements(), "Total elements should be 0");
    assertEquals(0, result.getContent().size(), "Content size should be 0");
  }

  /**
   * Verifies an empty page is returned when only admin users exist in the database.
   */
  @Test
  void givenOnlyAdminUsersInDatabaseWhenGetUsersThenReturnsEmptyPage() {
    Role adminRole = new Role("ADMIN");
    List<LogisticsUser> adminUsers = new ArrayList<>();
    adminUsers.add(
        new LogisticsUser(
            1L,
            "admin",
            "encodedPassword",
            "admin@example.com",
            fixedTimestamp,
            Set.of(adminRole, userRole)
        )
    );
    Page<LogisticsUser> adminUsersPage =
        new PageImpl<>(adminUsers, PageRequest.of(0, 10), adminUsers.size());
    when(logisticsUserRepository.findAll(pageable)).thenReturn(adminUsersPage);

    Page<UserResponseDto> result = logisticsUserService.getUsers(0, 10);

    verify(logisticsUserRepository, times(1)).findAll(pageable);
    assertNotNull(result);
    assertTrue(result.isEmpty(), "Result should be an empty page");
    assertEquals(0, result.getNumber(), "Current page number should be 0");
    assertEquals(0, result.getTotalPages(), "Total pages should be 0");
    assertEquals(0, result.getTotalElements(), "Total elements should be 0");
    assertEquals(0, result.getContent().size(), "Content size should be 0");
  }

  /**
   * Verifies admin users are filtered out from the returned page.
   */
  @Test
  void givenAdminAndUsersWhenGetUsersThenExcludeAdminUsers() {
    Page<LogisticsUser> pagedUsersWithAdmin = createPagedLogisticsUsers(0, 10, 3, true);
    when(logisticsUserRepository.findAll(pageable)).thenReturn(pagedUsersWithAdmin);

    Page<UserResponseDto> result = logisticsUserService.getUsers(0, 10);

    verify(logisticsUserRepository, times(1)).findAll(pageable);
    assertNotNull(result);
    // Using pagedUsers since it is a list without the admin user to compare the result.
    assertEquals(pagedUsers.getNumber(), result.getNumber());
    assertEquals(pagedUsers.getTotalPages(), result.getTotalPages());
    assertEquals(pagedUsers.getTotalElements(), result.getTotalElements());
    assertEquals(pagedUsers.getContent().size(), result.getContent().size());
    List<UserResponseDto> expectedDtos = pagedUsers.stream()
        .map(logisticsUserService::mapToUserResponseDto)
        .toList();
    assertEquals(expectedDtos, result.getContent());
  }

  /**
   * Verifies that correct number of users is returned given a specific page size.
   */
  @Test
  void givenSpecificPageSizeWhenGetUsersThenReturnCorrectNumberOfUsers() {
    pageable = PageRequest.of(0, 1);
    Page<LogisticsUser> sizeOnePage = createPagedLogisticsUsers(0, 1, 2, false);
    when(logisticsUserRepository.findAll(pageable)).thenReturn(sizeOnePage);

    Page<UserResponseDto> result = logisticsUserService.getUsers(0, 1);

    verify(logisticsUserRepository, times(1)).findAll(pageable);
    assertNotNull(result);
    assertEquals(sizeOnePage.getContent().size(), result.getContent().size());
    assertEquals(sizeOnePage.getNumber(), result.getNumber());
    assertEquals(sizeOnePage.getTotalPages(), result.getTotalPages());
    assertEquals(sizeOnePage.getTotalElements(), result.getTotalElements());
  }

  /**
   * Verifies that correct subset of users is returned given a specific page number.
   */
  @Test
  void givenSpecificPageNumberWhenGetUsersThenReturnCorrectPage() {
    pageable = PageRequest.of(1, 1);

    final LogisticsUser expectedUser = new LogisticsUser(
        2L,
        "testUser2",
        "encodedPassword",
        "test2@example.com",
        fixedTimestamp,
        Set.of(userRole)
    );

    Page<LogisticsUser> pageOne = createPagedLogisticsUsers(1, 1, 3, false);
    when(logisticsUserRepository.findAll(pageable)).thenReturn(pageOne);

    Page<UserResponseDto> result = logisticsUserService.getUsers(1, 1);

    verify(logisticsUserRepository, times(1)).findAll(pageable);
    assertNotNull(result);
    assertEquals(pageOne.getNumber(), result.getNumber());
    assertEquals(pageOne.getTotalPages(), result.getTotalPages());
    assertEquals(pageOne.getTotalElements(), result.getTotalElements());

    assertEquals(1, result.getContent().size());
    UserResponseDto expectedUserDto = logisticsUserService.mapToUserResponseDto(expectedUser);
    assertEquals(expectedUserDto, result.getContent().getFirst());
  }

  /**
   * Verifies the last page gets the remaining users correctly.
   */
  @Test
  void givenLastPageNumberWhenGetUsersThenReturnLastPage() {
    int totalUsers = 8;
    int pageSize = 3;
    int lastPageNumber = (totalUsers / pageSize);

    Pageable lastPageable = PageRequest.of(lastPageNumber, pageSize);
    Page<LogisticsUser> lastPage =
        createPagedLogisticsUsers(lastPageNumber, pageSize, totalUsers, false);
    when(logisticsUserRepository.findAll(lastPageable)).thenReturn(lastPage);

    Page<UserResponseDto> result = logisticsUserService.getUsers(lastPageNumber, pageSize);

    verify(logisticsUserRepository, times(1)).findAll(lastPageable);
    assertNotNull(result);
    assertEquals(lastPageNumber, result.getNumber());
    assertEquals((totalUsers + pageSize - 1) / pageSize, result.getTotalPages());
    assertEquals(totalUsers, result.getTotalElements());

    int remainingUsers = totalUsers % pageSize;
    assertEquals(remainingUsers, result.getContent().size());
    List<UserResponseDto> expectedDtos = lastPage.stream()
        .map(logisticsUserService::mapToUserResponseDto)
        .toList();
    assertEquals(expectedDtos, result.getContent());
  }

  /**
   * Creates a paginated response of {@link LogisticsUser} objects for testing purposes.
   * This method simulates the creation of a pageable dataset of users, optionally including
   * an admin user. The admin user is always assigned an ID of 1, and regular users are assigned
   * sequential IDs starting from 2 if the admin user is included.
   *
   * <p>The method calculates the subset of users based on the specified page and page size,
   * simulating pagination behavior consistent with typical database queries.
   *
   * @param page the page number to simulate (0-based index)
   * @param pageSize the number of users per page
   * @param totalUsers the total number of users to generate, including the admin user if specified
   * @param withAdmin whether to include an admin user in the list of users; if true, an admin user
   *                  will be added to the dataset with an ID of 1
   * @return a {@link Page} containing the {@link LogisticsUser} objects for the specified page
   *     and size
   */
  private Page<LogisticsUser> createPagedLogisticsUsers(
      int page,
      int pageSize,
      int totalUsers,
      boolean withAdmin) {

    List<LogisticsUser> usersList = new ArrayList<>();
    int currentId = 1; // Auto-increment starts at 1 as default behavior in PostgreSQL
    if (withAdmin) {
      Role adminRole = new Role("ADMIN");
      usersList.add(
          new LogisticsUser(
              (long) currentId,
              "admin",
              "encodedPassword",
              "admin@example.com",
              fixedTimestamp,
              Set.of(adminRole, userRole)
          )
      );
    }

    /*
     * Adjust the loop behavior based on whether an admin user is included.
     * If an admin user is present, totalUsers is reduced by 1 to account for the admin already
     * in the list. User IDs start at 2 in this case because the admin user is assigned ID 1.
     */
    for (int i = 1; i < totalUsers - (withAdmin ? 1 : 0) + 1; i++) {
      usersList.add(new LogisticsUser(
          (long) currentId++,
          "testUser" + i,
          "encodedPassword",
          "test" + i + "@example.com",
          fixedTimestamp,
          Set.of(userRole))
      );
    }

    // Simulate pagination
    int start = Math.min(page * pageSize, usersList.size());
    int end = Math.min(start + pageSize, usersList.size());
    List<LogisticsUser> pageContent = usersList.subList(start, end);

    return new PageImpl<>(pageContent, PageRequest.of(page, pageSize), pageContent.size());
  }
}
