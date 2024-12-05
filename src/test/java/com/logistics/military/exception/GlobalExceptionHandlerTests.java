package com.logistics.military.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
  @Mock private Clock clock;
  @Mock private BindingResult bindingResult;

  LocalDateTime fixedTimestamp = LocalDateTime.of(2024, 11, 17, 0, 0, 0, 0);
  Clock fixedClock =
      Clock.fixed(
          fixedTimestamp.atZone(ZoneId.systemDefault()).toInstant(),
          ZoneId.systemDefault());

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

    Map<String, Object> expectedResponse = new HashMap<>();
    expectedResponse.put("status", HttpStatus.BAD_REQUEST.value());
    expectedResponse.put("error", "Bad Request");
    expectedResponse.put("message", "Validation failed");

    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    expectedResponse.put("timestamp", fixedTimestamp);

    Map<String, String> details = new HashMap<>();
    details.put("username", "Username is required");
    expectedResponse.put("details", details);

    ResponseEntity<Map<String, Object>> response =
        globalExceptionHandler.handleValidationException(exception);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
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

    Map<String, String> expectedResponse = new HashMap<>();
    expectedResponse.put("error", "Invalid request body");
    expectedResponse.put("message", "Unrecognized field: user_id");

    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      ResponseEntity<Map<String, String>> response =
          globalExceptionHandler.handleUnknownProperty(exception);
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals(expectedResponse, response.getBody());

      assertThat(logCaptor.getWarnLogs())
          .anyMatch(log -> log.contains("Potential misuse:")
                           && log.contains("registration request"));
    }

    when(exception.getPropertyName()).thenReturn("unknownField");
    expectedResponse = new HashMap<>();
    expectedResponse.put("error", "Invalid request body");
    expectedResponse.put("message", "Unrecognized field: unknownField");

    try (LogCaptor logCaptor = LogCaptor.forClass(GlobalExceptionHandler.class)) {
      ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleUnknownProperty(
          exception);
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals(expectedResponse, response.getBody());

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

    Map<String, String> expectedResponse = new HashMap<>();
    expectedResponse.put("error", "User already exists");
    expectedResponse.put("message", "User already exists");

    ResponseEntity<Map<String, String>> response =
        globalExceptionHandler.handleUserAlreadyExists(exception);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
  }

  /**
   * Tests the {@code handleArgumentTypeMismatch} method. Verifies that a
   * {@link MethodArgumentTypeMismatchException} results in an HTTP 400 (Bad Request) response
   * with correct structure and content.
   */
  @Test
  void givenMethodArgumentTypeMismatchExceptionWhenHandleArgumentTypeMismatchThenBadRequest() {
    MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
    when(exception.getMessage()).thenReturn("Argument data type mismatch");

    Map<String, String> expectedResponse = new HashMap<>();
    expectedResponse.put("error", "Invalid argument data type");
    expectedResponse.put("message", "Argument data type mismatch");

    ResponseEntity<Map<String, String>> response =
        globalExceptionHandler.handleArgumentTypeMismatch(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(expectedResponse, response.getBody());
  }
}
