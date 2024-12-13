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
import org.springframework.dao.DataAccessException;
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
  public ResponseEntity<ResponseWrapper<String>> handleArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    return ResponseEntity.badRequest().body(
        ResponseWrapper.error("Invalid argument data type")
    );
  }

  /**
   * Handles exceptions for {@link UserCreationException} when an error occurs during
   * a save user to the database call.
   *
   * @param ex the {@link UserCreationException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 500
   */
  @ExceptionHandler(UserCreationException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserCreationException(
      UserCreationException ex) {
    String causeType =
        ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "Unknown Cause";
    logger.error("{} occurred during user creation with message: {}",
        causeType, ex.getMessage());
    return ResponseEntity.internalServerError().body(
        ResponseWrapper.error("User creation failed: " + ex.getMessage())
    );
  }

  /**
   * Handles {@link UserNotFoundException} when a user does not exist in the database.
   *
   * @param ex the {@link UserNotFoundException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 404
   */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserNotFoundException(
      UserNotFoundException ex) {
    logger.error("UserNotFoundException: {} | Operation: {}", ex.getMessage(), ex.getOperation());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ResponseWrapper.error(ex.getMessage())
    );
  }

  /**
   * Handles {@link UnauthorizedOperationException} when an unauthorized user attempts an operation
   * on an admin user. The error message must be the exact format as a user not found error message
   * to prevent inference that id used belongs to an admin.
   *
   * @param ex the {@link UnauthorizedOperationException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 404
   */
  @ExceptionHandler(UnauthorizedOperationException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUnauthorizedOperationException(
      UnauthorizedOperationException ex) {
    logger.error(ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ResponseWrapper.error(String.format("User with id %d does not exist", ex.getId()))
    );
  }

  /**
   * Handles {@link UserDeletionException} when an error occurs during a deletion of a user.
   *
   * @param ex the {@link UserDeletionException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 409
   */
  @ExceptionHandler(UserDeletionException.class)
  public ResponseEntity<ResponseWrapper<String>> handleUserDeletionException(
      UserDeletionException ex) {
    String causeType =
        ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "Unknown Cause";
    logger.error("{} occurred during user deletion with message: {}",
        causeType, ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ResponseWrapper.error(ex.getMessage())
    );
  }

  /**
   * Handles generic database-related exceptions for a {@link DataAccessException} occurrence.
   *
   * @param ex the {@link DataAccessException} containing the error details
   * @return a {@link ResponseEntity} containing the error response with the HTTP status of 500
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ResponseWrapper<String>> handleDataAccessException(
      DataAccessException ex) {
    logger.error("Database error occurred", ex);
    return ResponseEntity.internalServerError().body(
        ResponseWrapper.error("An unexpected database error occurred")
    );
  }
}
