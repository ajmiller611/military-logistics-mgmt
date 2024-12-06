package com.logistics.military.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.logistics.military.response.ResponseWrapper;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

  /**
   * Handles validation exceptions thrown when method arguments fail validation.
   * Captures details about invalid fields and provides a structured error response.
   *
   * @param ex the {@link MethodArgumentNotValidException} containing validation error details
   * @return a {@link ResponseEntity} containing the error response with HTTP status 400
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResponseWrapper<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException ex) {

    Map<String, String> details = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      details.put(fieldName, errorMessage);
    });

    ResponseWrapper<Map<String, String>> response = ResponseWrapper.error("Validation failed");
    response.setData(details);
    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handles exceptions for unrecognized properties in JSON requests.
   * If the unrecognized property resembles a user ID field, logs a warning about
   * potential misuse then provides a structured error response.
   *
   * @param ex the {@link UnrecognizedPropertyException} containing details about the unrecognized
   *           property
   * @return a {@link ResponseEntity} containing the error response with HTTP status 400
   */
  @ExceptionHandler(UnrecognizedPropertyException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUnknownProperty(
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

    return ResponseEntity.badRequest().body(
        ResponseWrapper.error(String.format("Unrecognized field named '%s'.", propertyName))
    );
  }

  /**
   * Handles exceptions thrown when a user already exists during registration then provides a
   * structured error response.
   *
   * @param ex the {@link UserAlreadyExistsException} containing error details about the existing
   *           user conflict
   * @return a {@link ResponseEntity} containing the error response with HTTP status 409
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserAlreadyExists(
      UserAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ResponseWrapper.error(ex.getMessage())
    );
  }

  /**
   * Handles exceptions for {@link MethodArgumentTypeMismatchException} when arguments are
   * the wrong type.
   *
   * @param ex the {@link MethodArgumentTypeMismatchException} containing error details about
   *           the type mismatch
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 400
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ResponseWrapper<Object>> handleArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    return ResponseEntity.badRequest().body(
        ResponseWrapper.error("Invalid argument data type")
    );
  }
}
