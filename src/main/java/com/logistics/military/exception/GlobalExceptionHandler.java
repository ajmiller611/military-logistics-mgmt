package com.logistics.military.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for handling application-wide exceptions.
 * This class provides specific handlers for validation errors, unrecognized JSON fields,
 * and user existence conflicts.
 *
 * <p>All exception handlers in this class return consistent error responses with HTTP
 * status codes and detailed error messages, ensuring a standardized format for error handling.
 * </p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Clock clock;

  /**
   * Handles validation exceptions thrown when method arguments fail validation.
   * Captures details about invalid fields and provides a structured error response.
   *
   * @param ex the {@link MethodArgumentNotValidException} containing validation error details
   * @return a {@link ResponseEntity} containing the error response map with HTTP status 400
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException ex) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
    errorResponse.put("error", "Bad Request");
    errorResponse.put("message", "Validation failed");
    errorResponse.put("timestamp", LocalDateTime.now(clock));

    Map<String, String> details = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      details.put(fieldName, errorMessage);
    });
    errorResponse.put("details", details);
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles exceptions for unrecognized properties in JSON requests.
   * If the unrecognized property resembles a user ID field, logs a warning about
   * potential misuse then provides a structured error response.
   *
   * @param ex the {@link UnrecognizedPropertyException} containing details about the unrecognized
   *           property
   * @return a {@link ResponseEntity} containing the error response map with HTTP status 400
   */
  @ExceptionHandler(UnrecognizedPropertyException.class)
  public ResponseEntity<Map<String, String>> handleUnknownProperty(
      UnrecognizedPropertyException ex) {
    String propertyName = ex.getPropertyName();

    List<String> userIdFields = Arrays.asList(
        "userId", "userid", "userID", "user_Id", "user_id", "user-Id", "user-id", "user-ID");

    if (userIdFields.contains(propertyName)) {
      logger.warn("Potential misuse: A '{}' field was found in a registration request.",
          propertyName);
    } else {
      logger.warn("Unrecognized field named '{}' found in a registration request.", propertyName);
    }

    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Invalid request body");
    errorResponse.put("message", "Unrecognized field: " + ex.getPropertyName());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Handles exceptions thrown when a user already exists during registration then provides a
   * structured error response.
   *
   * @param ex the {@link UserAlreadyExistsException} containing error details about the existing
   *           user conflict
   * @return a {@link ResponseEntity} containing the error response map with HTTP status 409
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<Map<String, String>> handleUserAlreadyExists(
      UserAlreadyExistsException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "User already exists");
    errorResponse.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }
}
