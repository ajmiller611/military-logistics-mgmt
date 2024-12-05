package com.logistics.military.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.logistics.military.response.ResponseWrapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    assertEquals("Validation Failed", response.getBody().getMessage());
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

    ResponseEntity<ResponseWrapper<Object>> response =
        globalExceptionHandler.handleArgumentTypeMismatch(exception);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("error", Objects.requireNonNull(response.getBody()).getStatus());
    assertEquals("Invalid argument data type", response.getBody().getMessage());
  }
}
