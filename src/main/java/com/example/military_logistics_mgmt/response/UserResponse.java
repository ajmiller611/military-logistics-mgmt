package com.example.military_logistics_mgmt.response;

import com.example.military_logistics_mgmt.model.User;
import lombok.Data;

/**
 * A response wrapper class for user-related operations. This class encapsulates
 * the result of an operation involving a {@link User}, along with an optional
 * error message if the operation fails.
 */
@Data
public class UserResponse {

    private User user;
    private String error;

    /**
     * Constructs a new {@code UserResponse} with the provided user and error message.
     *
     * @param user The {@link User} object, can be null if an error occurred.
     * @param error A string describing the error, can be null if the operation was successful.
     */
    public UserResponse(User user, String error) {
        this.user = user;
        this.error = error;
    }
}
