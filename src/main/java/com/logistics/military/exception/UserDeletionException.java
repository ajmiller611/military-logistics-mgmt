package com.logistics.military.exception;

/**
 * Custom exception thrown when an error occurs during the deletion of a user from the database.
 */
public class UserDeletionException extends RuntimeException {

  /**
   * Constructs a new {@code UserDeletionException} with the specified detail message.
   *
   * @param message the detail message explaining the reason for the exception
   * @param cause the cause of the exception
   */
  public UserDeletionException(String message, Throwable cause) {
    super(message, cause);
  }
}
