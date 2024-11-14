package com.logistics.military.response;

import com.logistics.military.model.LogisticsUser;
import lombok.Data;

/**
 * A response wrapper class for user-related operations. This class encapsulates the result of an
 * operation involving a {@link LogisticsUser}, along with an optional error message if the
 * operation fails.
 */
@Data
public class LogisticsUserResponse {

  private LogisticsUser user;
  private String error;

  /**
   * Constructs a new {@link LogisticsUserResponse} with the provided user and error message.
   *
   * @param user  The {@link LogisticsUser} object, can be null if an error occurred.
   * @param error A string describing the error, can be null if the operation was successful.
   */
  public LogisticsUserResponse(LogisticsUser user, String error) {
    this.user = user;
    this.error = error;
  }
}
