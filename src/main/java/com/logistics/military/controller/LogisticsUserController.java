package com.logistics.military.controller;

import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.response.PaginatedData;
import com.logistics.military.response.ResponseWrapper;
import com.logistics.military.service.LogisticsUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling user-related HTTP requests.
 *
 * <p>This controller exposes endpoints for users with the 'user' role access level.
 * It also facilitates interaction with the {@link LogisticsUserService} for user-related
 * operations.
 * </p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class LogisticsUserController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final LogisticsUserService logisticsUserService;

  /**
   * Retrieves a paginated list of users.
   *
   * <p>This endpoint allows clients to fetch users in a paginated manner. By default,
   * the first page with a size of 10 users is returned if no query parameters are provided.
   * Clients can specify the desired page number and page size using query parameters.</p>
   *
   * <p>If invalid parameters are provided (e.g., negative page number or non-positive size), the
   * response includes a bad request status with an appropriate error message. In case of server
   * errors, the response contains an error message with a corresponding HTTP status code.</p>
   *
   * @param page the page number to retrieve, starting from 0 (default: 0)
   * @param size the number of users to retrieve per page (default: 10)
   * @return a {@link ResponseEntity} containing a {@link ResponseWrapper} with a
   *     paginated list of {@link UserResponseDto}, or an error message in case of invalid input
   *     or server errors.
   */
  @GetMapping({"/", ""})
  public ResponseEntity<ResponseWrapper<PaginatedData<UserResponseDto>>> getUsers(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    // Validate parameters page and size
    if (page < 0) {
      return ResponseEntity.badRequest().body(
          ResponseWrapper.error("Page number must not be negative"));
    }
    if (size <= 0) {
      return ResponseEntity.badRequest().body(
          ResponseWrapper.error("Size must be a positive number")
      );
    }

    try {
      Page<UserResponseDto> pagedUsers = logisticsUserService.getUsers(page, size);
      PaginatedData<UserResponseDto> paginatedData = new PaginatedData<>(
          pagedUsers.getContent(),
          pagedUsers.getNumber(),
          pagedUsers.getTotalPages(),
          pagedUsers.getTotalElements()
      );

      logger.info("Retrieved users for page {}: {} users ({} total pages, {} total users)",
          pagedUsers.getNumber(),
          pagedUsers.getContent().size(),
          pagedUsers.getTotalPages(),
          pagedUsers.getTotalElements());

      return ResponseEntity.ok(
          ResponseWrapper.success(paginatedData, "Users retrieved successfully"));
    } catch (DataAccessException e) {
      logger.error("Database error occurred while fetching users: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          ResponseWrapper.error("An unexpected error occurred while fetching user data")
      );
    } catch (Exception e) {
      logger.error("Unexpected error occurred: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          ResponseWrapper.error("An unexpected error occurred")
      );
    }
  }
}
