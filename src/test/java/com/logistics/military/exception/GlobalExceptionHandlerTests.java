package com.logistics.military.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.logistics.military.response.ResponseWrapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * This test class verifies the behavior of exception handling methods in
 * {@code GlobalExceptionHandler} to ensure they produce correct HTTP responses and log messages
 * based on specific exception types.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTests {

  @InjectMocks private GlobalExceptionHandler globalExceptionHandler;
  @Mock private BindingResult bindingResult;

  /**
   * Tests the {@code handleValidationException} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link MethodArgumentNotValidException} results in an HTTP 400 (Bad Request)
   * response with the correct structure and content.
   */
  @Test
  void givenMethodArgumentNotValidExceptionWhenHandleValidationExceptionThenBadRequest() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);
    FieldError fieldError = mock(FieldError.class);
    when(fieldError.getField()).thenReturn("username");
    when(fieldError.getDefaultMessage()).thenReturn("Username is required");
    when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ResponseWrapper<Map<String, String>>> response =
        globalExceptionHandler.handleValidationException(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
    assertEquals("Validation failed", response.getBody().getMessage());
    assertEquals("Username is required", response.getBody().getData().get("username"));
  }

  /**
   * Tests the {@code handleUnknownProperty} method in {@link GlobalExceptionHandler}.
   * Verifies that an {@link UnrecognizedPropertyException} results in an HTTP 400 (Bad Request)
   * response with the correct structure and content.
   *
   * <p>This test also verifies that a warning log is generated when the unrecognized field
   * resembles a user ID field.
   * </p>
   */
  @Test
  void givenUnrecognizedPropertyExceptionWhenHandleUnknownPropertyThenBadRequest() {
    UnrecognizedPropertyException exception = mock(UnrecognizedPropertyException.class);
    when(exception.getPropertyName()).thenReturn("user_id");

    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUnknownProperty(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("Unrecognized field named 'user_id'.", response.getBody().getMessage());

      assertThat(logCaptor.getWarnLogs())
          .anyMatch(log -> log.contains("Potential misuse:")
                           && log.contains("registration request"));
    }

    when(exception.getPropertyName()).thenReturn("unknownField");

    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUnknownProperty(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("Unrecognized field named 'unknownField'.", response.getBody().getMessage());

      assertThat(logCaptor.getWarnLogs())
          .anyMatch(log -> log.contains("Unrecognized field named")
                           && log.contains("registration request"));
    }
  }

  /**
   * Tests the {@code handleUserAlreadyExists} method in {@link GlobalExceptionHandler}.
   * Verifies that a {@link UserAlreadyExistsException} results in an HTTP 409 (Conflict)
   * response with the correct structure and content.
   */
  @Test
  void givenUserAlreadyExistsExceptionWhenHandleUserAlreadyExistsThenConflict() {
    UserAlreadyExistsException exception = new UserAlreadyExistsException("User already exists");

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleUserAlreadyExists(exception);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
    assertThat(response.getBody().getMessage()).contains("User already exists");
  }

  /**
   * Tests the {@code handleArgumentTypeMismatch} method. Verifies that a
   * {@link MethodArgumentTypeMismatchException} results in an HTTP 400 (Bad Request) response
   * with correct structure and content.
   */
  @Test
  void givenMethodArgumentTypeMismatchExceptionWhenHandleArgumentTypeMismatchThenBadRequest() {
    MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

    ResponseEntity<ResponseWrapper<String>> response =
        globalExceptionHandler.handleArgumentTypeMismatch(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
    assertEquals("Invalid argument data type", response.getBody().getMessage());
  }

  /**
   * Tests the {@code handleUserCreationException} method. Verifies that a
   * {@link UserCreationException} results in an HTTP 500 (internal server error) response
   * with correct structure and content.
   */
  @Test
  void givenUserCreationExceptionWhenHandleUserCreationExceptionThenInternalServerError() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataAccessException cause = mock(DataAccessException.class);
      UserCreationException exception =
          new UserCreationException("User Creation Exception message", cause);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUserCreationException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertThat(response.getBody().getMessage()).contains("User creation failed:");

      assertThat(logCaptor.getErrorLogs())
          .anyMatch(log ->
              log.contains("DataAccessException")
                  && log.contains("occurred during user creation with message:")
                  && log.contains("User Creation Exception message"));

      // Unknown cause
      exception = new UserCreationException("User Creation Exception message", null);

      response = globalExceptionHandler.handleUserCreationException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertThat(response.getBody().getMessage()).contains("User creation failed:");

      assertThat(logCaptor.getErrorLogs())
          .anyMatch(log ->
              log.contains("Unknown Cause")
                  && log.contains("occurred during user creation with message:")
                  && log.contains("User Creation Exception message"));
    }
  }

  /**
   * Tests the {@code handleUserNotFoundException} method. Verifies that a
   * {@link UserNotFoundException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenUserNotFoundExceptionWhenHandleUserNotFoundExceptionThenNotFound() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      UserNotFoundException exception = mock(UserNotFoundException.class);
      when(exception.getMessage()).thenReturn("User with id 3 does not exist");
      when(exception.getOperation()).thenReturn("deleteUser");

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUserNotFoundException(exception);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("User with id 3 does not exist", response.getBody().getMessage());

      assertThat(logCaptor.getErrorLogs())
          .contains("UserNotFoundException: User with id 3 does not exist | Operation: deleteUser");
    }
  }

  /**
   * Tests the {@code handleUnauthorizedOperationException} method. Verifies that a
   * {@link UnauthorizedOperationException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenUnauthorizedOperationExceptionWhenHandleUnauthorizedOperationExceptionThenNotFound() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      UnauthorizedOperationException exception = mock(UnauthorizedOperationException.class);
      when(exception.getMessage()).thenReturn("User with id 3 does not exist");
      when(exception.getId()).thenReturn(3L);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUnauthorizedOperationException(exception);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("User with id 3 does not exist", response.getBody().getMessage());

      assertThat(logCaptor.getErrorLogs()).contains("User with id 3 does not exist");
    }
  }

  /**
   * Tests the {@code handleUserDeletionException} method. Verifies that a
   * {@link UserDeletionException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenUserDeletionExceptionWhenHandleUserDeletionExceptionThenConflict() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataIntegrityViolationException cause = mock(DataIntegrityViolationException.class);
      UserDeletionException exception =
          new UserDeletionException("Entity has constraints", cause);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleUserDeletionException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertThat(response.getBody().getMessage()).contains("Entity has constraints");

      assertThat(logCaptor.getErrorLogs())
          .anyMatch(log ->
              log.contains("DataIntegrityViolationException")
                  && log.contains("occurred during user deletion with message:")
                  && log.contains("Entity has constraints"));

      // Unknown cause
      exception = new UserDeletionException("Entity has constraints", null);

      response = globalExceptionHandler.handleUserDeletionException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertThat(response.getBody().getMessage()).contains("Entity has constraints");

      assertThat(logCaptor.getErrorLogs())
          .anyMatch(log ->
              log.contains("Unknown Cause")
                  && log.contains("occurred during user deletion with message:")
                  && log.contains("Entity has constraints"));
    }
  }

  /**
   * Tests the {@code handleDataIntegrityViolationException} method. Verifies that a
   * {@link DataIntegrityViolationException} results in an HTTP 409 (conflict) response
   * with correct structure and content.
   */
  @Test
  void givenDataIntegrityViolationExceptionWhenHandleDataIntegrityViolationExceptionThenConflict() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleDataIntegrityViolationException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("A conflict occurred due to database constraints",
          response.getBody().getMessage());

      assertThat(logCaptor.getErrorLogs()).contains("Data integrity violation occurred");
    }
  }

  /**
   * Tests the {@code handleOptimisticLockingFailureException} method. Verifies that a
   * {@link OptimisticLockingFailureException} results in an HTTP 409 (conflict) response
   * with correct structure and content.
   */
  @Test
  void givenOptimisticLockingFailureWhenHandleOptimisticLockingFailureExceptionThenConflict() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      OptimisticLockingFailureException exception = mock(OptimisticLockingFailureException.class);

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleOptimisticLockingFailureException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("Concurrency conflict: another user updated the record",
          response.getBody().getMessage());

      assertThat(logCaptor.getErrorLogs()).contains("Optimistic locking conflict");
    }
  }

  /**
   * Tests the {@code handleDataAccessException} method. Verifies that a
   * {@link DataAccessException} results in an HTTP 404 (not found) response
   * with correct structure and content.
   */
  @Test
  void givenDataAccessExceptionWhenHandleDataAccessExceptionThenInternalServerError() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      DataAccessException exception = mock(DataAccessException.class);
      when(exception.getMessage()).thenReturn("An unexpected database error occurred");

      ResponseEntity<ResponseWrapper<String>> response =
          globalExceptionHandler.handleDataAccessException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("An unexpected database error occurred", response.getBody().getMessage());

      assertThat(logCaptor.getErrorLogs()).contains("Database error occurred");
    }
  }

  /**
   * Tests the {@code handleHandlerMethodValidationException} method. Verifies that when a
   * {@link ConstraintViolationException} occurs from a Jakarta annotation, a
   * {@link HandlerMethodValidationException} is thrown and wraps a
   * {@link ConstraintViolationException}. After the exception is handled, it results in an
   * HTTP 400 (bad request) response with correct structure and content.
   */
  @Test
  void givenConstraintViolationExceptionWhenHandleHandlerMethodValidationExceptionThenBadRequest() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      Path mockPath = mock(Path.class);
      when(mockPath.toString()).thenReturn("deleteUser.id");

      ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);
      when(mockViolation.getMessage()).thenReturn("User id must be greater than zero");
      when(mockViolation.getInvalidValue()).thenReturn(0L);

      ConstraintViolationException constraintViolationException = new ConstraintViolationException(
          Set.of(mockViolation));

      HandlerMethodValidationException mockHandlerMethodValidationException =
          mock(HandlerMethodValidationException.class);
      when(mockHandlerMethodValidationException.getCause())
          .thenReturn(constraintViolationException);

      ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> response =
          globalExceptionHandler
              .handleHandlerMethodValidationException(mockHandlerMethodValidationException);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("Validation failed", response.getBody().getMessage());
      assertEquals("id", response.getBody().getData().getFirst().get("field"));
      assertEquals("User id must be greater than zero",
          response.getBody().getData().getFirst().get("message"));
      assertEquals(0L, response.getBody().getData().getFirst().get("invalidValue"));

      assertThat(logCaptor.getErrorLogs())
          .contains("HandlerMethodValidationException caught with cause: "
              + "ConstraintViolationException");

      // Set up when cause is null
      when(mockHandlerMethodValidationException.getCause()).thenReturn(null);

      globalExceptionHandler
          .handleHandlerMethodValidationException(mockHandlerMethodValidationException);

      // Assert that a null cause will return Unknown Cause name in the log
      assertThat(logCaptor.getErrorLogs())
          .contains("HandlerMethodValidationException caught with cause: "
              + "Unknown Cause");
    }
  }

  /**
   * Tests the {@code handleConstraintViolationException} method. Verifies that a
   * {@link ConstraintViolationException} results in an HTTP 400 (bad request) response
   * with correct structure and content.
   */
  @Test
  void givenConstraintViolationExceptionWhenHandleConstraintViolationExceptionThenBadRequest() {
    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      Path mockPath = mock(Path.class);
      when(mockPath.toString()).thenReturn("deleteUser.id");

      ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
      when(mockViolation.getPropertyPath()).thenReturn(mockPath);
      when(mockViolation.getMessage()).thenReturn("User id must be greater than zero");
      when(mockViolation.getInvalidValue()).thenReturn(0L);

      ConstraintViolationException mockConstraintViolationException =
          mock(ConstraintViolationException.class);
      when(mockConstraintViolationException.getConstraintViolations())
          .thenReturn(Set.of(mockViolation));

      ResponseEntity<ResponseWrapper<List<Map<String, Object>>>> response =
          globalExceptionHandler
              .handleConstraintViolationException(mockConstraintViolationException);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
      assertEquals("Validation failed", response.getBody().getMessage());

      List<Map<String, Object>> errors = response.getBody().getData();
      assertNotNull(errors);
      assertEquals(1, errors.size());

      Map<String, Object> error = errors.getFirst();
      assertEquals("id", error.get("field"));
      assertEquals("User id must be greater than zero", error.get("message"));
      assertEquals(0L, error.get("invalidValue"));

      assertThat(logCaptor.getErrorLogs())
          .contains("Validation failure: deleteUser failed with id having invalid value of 0 with "
              + "message User id must be greater than zero");
    }
  }
}
