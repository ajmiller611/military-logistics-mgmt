package com.logistics.military.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.exception.RoleNotFoundException;
import com.logistics.military.exception.UnauthorizedOperationException;
import com.logistics.military.exception.UserNotFoundException;
import com.logistics.military.model.LogisticsUser;
import com.logistics.military.model.Role;
import com.logistics.military.repository.LogisticsUserRepository;
import com.logistics.military.repository.RoleRepository;
import com.logistics.military.service.LogisticsUserService;
import java.time.LocalDateTime;
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
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests for the {@link LogisticsUserService} class.
 *
 * <p>This test class validates the {@code LogisticsUserService}'s read functionalities,
 * focusing on correct implementation of user retrieval operations, pagination logic,
 * and proper handling of edge cases and exceptions.
 * </p>
 *
 * <h2>Key Features Tested</h2>
 * <ul>
 *   <li>
 *     <strong>Pagination and Filtering:</strong> Ensures accurate pagination behavior,
 *     including retrieving non-admin users, returning correct page content, and handling
 *     empty datasets appropriately.
 *   </li>
 *   <li>
 *     <strong>Role-Based Exclusion:</strong> Confirms that admin users are excluded
 *     from user retrieval operations.
 *   </li>
 *   <li>
 *     <strong>Error Handling:</strong> Verifies exceptions such as {@link RoleNotFoundException},
 *     {@link UserNotFoundException}, and {@link UnauthorizedOperationException} are thrown
 *     and handled correctly when invalid operations are performed.
 *   </li>
 *   <li>
 *     <strong>DTO Mapping:</strong> Validates conversion of {@link LogisticsUser} objects
 *     to {@link UserResponseDto} objects, ensuring consistency in returned data.
 *   </li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LogisticsUserServiceReadTests {

  @InjectMocks private LogisticsUserService logisticsUserService;
  @Mock private LogisticsUserRepository logisticsUserRepository;
  @Mock private RoleRepository roleRepository;

  Role adminRole;
  Role userRole;
  Long userId;
  LogisticsUser user;
  Pageable pageable;
  Page<LogisticsUser> pagedUsers;

  /** Initializes test data before each test case. */
  @BeforeEach
  void setUp() {
    userId = 2L;
    adminRole = new Role(1, "ADMIN");
    userRole = new Role(2, "USER");
    user = new LogisticsUser(
        userId,
        "testUser",
        "password",
        "test@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );

    // Create a test page of users for use in each test case
    pageable = PageRequest.of(0, 10);
    pagedUsers = createPagedLogisticsUsers(0, 10, 2);
  }

  /** Ensures {@link LogisticsUser} objects are converted to {@link UserResponseDto} objects. */
  @Test
  void givenLogisticsUserWhenMapToUserResponseDtoThenReturnUserResponseDto() {
    UserResponseDto responseDto = logisticsUserService.mapToUserResponseDto(user);

    assertNotNull(responseDto);
    assertEquals(user.getUserId(), responseDto.getUserId());
    assertEquals(user.getUsername(), responseDto.getUsername());
    assertEquals(user.getEmail(), responseDto.getEmail());
  }

  /** Verify an {@link RoleNotFoundException} is thrown when the 'ADMIN' role does not exist. */
  @Test
  void givenRoleNotFoundWhenGetUsersThenThrowRoleNotFoundException() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.empty());

    assertThrows(RoleNotFoundException.class,
        () -> logisticsUserService.getUsers(0, 10),
        "Expected getUsers to throw a RoleNotFoundException for role not found");
  }

  /**
   * Verifies a valid page and size request retrieves non-admin users, converts them to DTOs,
   * and returns a paginated result.
   */
  @Test
  void givenValidPageAndSizeWhenGetUsersThenReturnPageOfUsers() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    when(logisticsUserRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(pagedUsers);

    Page<UserResponseDto> fetchedUsers = logisticsUserService.getUsers(0, 10);

    verify(logisticsUserRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers);
    assertEquals(pagedUsers.getNumber(), fetchedUsers.getNumber());
    assertEquals(pagedUsers.getTotalPages(), fetchedUsers.getTotalPages());
    assertEquals(pagedUsers.getTotalElements(), fetchedUsers.getTotalElements());
    assertEquals(pagedUsers.getContent().size(), fetchedUsers.getContent().size());
    assertTrue(fetchedUsers.getContent().stream()
        .noneMatch(dto -> "admin".equals(dto.getUsername())));
    assertIterableEquals(pagedUsers.getContent().stream()
        .map(logisticsUserService::mapToUserResponseDto)
        .toList(), fetchedUsers.getContent());
  }

  /** Verifies when no users exist then an empty page is returned. */
  @Test
  void givenNoUsersWhenGetUsersThenReturnsEmptyPage() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    Page<LogisticsUser> emptyPage =
        new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
    when(logisticsUserRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(emptyPage);

    Page<UserResponseDto> fetchedUsers
        = logisticsUserService.getUsers(0, 10);

    verify(logisticsUserRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers, "Fetched users should not be null; "
        + "the service should always return a page, even if empty.");
    assertTrue(fetchedUsers.isEmpty(), "Fetched users should be an empty page");
    assertEquals(0, fetchedUsers.getNumber(), "Current page number should be 0");
    assertEquals(0, fetchedUsers.getTotalPages(), "Total pages should be 0");
    assertEquals(0, fetchedUsers.getTotalElements(), "Total elements should be 0");
    assertEquals(0, fetchedUsers.getContent().size(), "Content size should be 0");
  }

  /** Verifies correct number of users is returned given a specific page size. */
  @Test
  void givenSpecificPageSizeWhenGetUsersThenReturnCorrectNumberOfUsers() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    pageable = PageRequest.of(0, 1);
    Page<LogisticsUser> sizeOnePage = createPagedLogisticsUsers(0, 1, 2);
    when(logisticsUserRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(sizeOnePage);

    Page<UserResponseDto> fetchedUsers = logisticsUserService.getUsers(0, 1);

    verify(logisticsUserRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers);
    assertEquals(sizeOnePage.getContent().size(), fetchedUsers.getContent().size());
    assertEquals(sizeOnePage.getNumber(), fetchedUsers.getNumber());
    assertEquals(sizeOnePage.getTotalPages(), fetchedUsers.getTotalPages());
    assertEquals(sizeOnePage.getTotalElements(), fetchedUsers.getTotalElements());
  }

  /** Verifies correct subset of users is returned given a specific page number. */
  @Test
  void givenSpecificPageNumberWhenGetUsersThenReturnCorrectPage() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    pageable = PageRequest.of(1, 1);

    final LogisticsUser expectedUser = new LogisticsUser(
        3L,
        "testUser2",
        "encodedPassword",
        "test2@example.com",
        LocalDateTime.now(),
        Set.of(userRole)
    );

    Page<LogisticsUser> pageOne = createPagedLogisticsUsers(1, 1, 3);
    when(logisticsUserRepository.findAllWithoutRole(pageable, adminRole)).thenReturn(pageOne);

    Page<UserResponseDto> fetchedUsers = logisticsUserService.getUsers(1, 1);

    verify(logisticsUserRepository, times(1))
        .findAllWithoutRole(pageable, adminRole);
    assertNotNull(fetchedUsers);
    assertEquals(pageOne.getNumber(), fetchedUsers.getNumber());
    assertEquals(pageOne.getTotalPages(), fetchedUsers.getTotalPages());
    assertEquals(pageOne.getTotalElements(), fetchedUsers.getTotalElements());

    assertEquals(1, fetchedUsers.getContent().size());
    UserResponseDto expectedUserDto = logisticsUserService.mapToUserResponseDto(expectedUser);
    assertEquals(expectedUserDto, fetchedUsers.getContent().getFirst());
  }

  /** Verifies the last page gets the remaining users correctly. */
  @Test
  void givenLastPageNumberWhenGetUsersThenReturnLastPage() {
    when(roleRepository.findByAuthority("ADMIN")).thenReturn(Optional.of(adminRole));
    int totalUsers = 8;
    int pageSize = 3;
    int lastPageNumber = (totalUsers / pageSize);

    Pageable lastPageable = PageRequest.of(lastPageNumber, pageSize);
    Page<LogisticsUser> lastPage =
        createPagedLogisticsUsers(lastPageNumber, pageSize, totalUsers);
    when(logisticsUserRepository.findAllWithoutRole(lastPageable, adminRole)).thenReturn(lastPage);

    Page<UserResponseDto> fetchedUsers = logisticsUserService.getUsers(lastPageNumber, pageSize);

    verify(logisticsUserRepository, times(1))
        .findAllWithoutRole(lastPageable, adminRole);
    assertNotNull(fetchedUsers);
    assertEquals(lastPageNumber, fetchedUsers.getNumber());
    assertEquals((totalUsers + pageSize - 1) / pageSize, fetchedUsers.getTotalPages());
    assertEquals(totalUsers, fetchedUsers.getTotalElements());

    int remainingUsers = totalUsers % pageSize;
    assertEquals(remainingUsers, fetchedUsers.getContent().size());
    List<UserResponseDto> expectedDtos = lastPage.stream()
        .map(logisticsUserService::mapToUserResponseDto)
        .toList();
    assertEquals(expectedDtos, fetchedUsers.getContent());
  }

  /**
   * Creates a paginated response of {@link LogisticsUser} objects for testing purposes.
   * This method simulates the creation of a pageable dataset of users.
   * This simulates an admin user exists assigned an ID of 1, and regular users are assigned
   * sequential IDs starting from 2.
   *
   * <p>The method calculates the subset of users based on the specified page and page size,
   * simulating pagination behavior consistent with typical database queries.
   *
   * @param page the page number to simulate (0-based index)
   * @param pageSize the number of users per page
   * @param totalUsers the total number of users to generate
   * @return a {@link Page} containing the {@link LogisticsUser} objects for the specified page
   *     and size
   */
  private Page<LogisticsUser> createPagedLogisticsUsers(int page, int pageSize, int totalUsers) {
    List<LogisticsUser> usersList = new ArrayList<>();

    int currentId = 1; // Start with ID 1 to account for an admin user with ID 1 existing.
    for (int i = 1; i <= totalUsers; i++) {
      usersList.add(new LogisticsUser(
          (long) ++currentId,
          "testUser" + i,
          "encodedPassword",
          "test" + i + "@example.com",
          LocalDateTime.now(),
          Set.of(userRole))
      );
    }

    // Simulate pagination
    int start = Math.min(page * pageSize, usersList.size());
    int end = Math.min(start + pageSize, usersList.size());
    List<LogisticsUser> pageContent = usersList.subList(start, end);

    return new PageImpl<>(pageContent, PageRequest.of(page, pageSize), pageContent.size());
  }

  /** Verifies that a valid id returns the user's data. */
  @Test
  void givenValidIdWhenGetUserByIdThenReturnUser() {
    Optional<LogisticsUser> optionalLogisticsUser = Optional.of(user);
    when(logisticsUserRepository.findById(userId)).thenReturn(optionalLogisticsUser);

    UserResponseDto result = logisticsUserService.getUserById(userId);

    verify(logisticsUserRepository, times(1)).findById(userId);
    assertNotNull(result);
    assertEquals(user.getUserId(), result.getUserId());
    assertEquals(user.getUsername(), result.getUsername());
    assertEquals(user.getEmail(), result.getEmail());
  }

  /** Verifies that an admin user ID throws {@link UnauthorizedOperationException}. */
  @Test
  void givenAdminIdTypeWhenGetUserByIdThenThrowsUnauthorizedOperationException() {
    Long adminUserId = 1L;
    LogisticsUser adminUser = mock(LogisticsUser.class);
    when(adminUser.hasRole("ADMIN")).thenReturn(true);

    when(logisticsUserRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

    UnauthorizedOperationException exception = assertThrows(UnauthorizedOperationException.class,
        () -> logisticsUserService.getUserById(adminUserId));

    verify(logisticsUserRepository, times(1)).findById(1L);
    assertNotNull(exception);
    assertEquals(
        String.format("Unauthorized user cannot update admin user with id %d", adminUserId),
        exception.getMessage());
    assertEquals(adminUserId, exception.getId());
  }
}
