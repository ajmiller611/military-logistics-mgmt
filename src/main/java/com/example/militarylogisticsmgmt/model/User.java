package com.example.militarylogisticsmgmt.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Represents a user in the system. This entity is mapped to the 'users' table in the database. It
 * contains fields for user ID, username, password, email, and creation timestamp.
 */
@Data
@AllArgsConstructor
@Entity
@Table(name = "users") // Specify the table name to avoid reserved keyword issues
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userId;
  private String username;

  // Increase length to accommodate encoded passwords, which can be longer than the original input
  @Column(length = 512)
  private String password;

  private String email;
  private LocalDateTime createdAt;

  /**
   * Default constructor required by JPA for entity instantiation.
   */
  public User() {
    // Empty constructor for JPA
  }

  // Override toString to prevent logging sensitive information, such as the password.
  @Override
  public String toString() {
    return "User{"
        + "userId=" + userId
        + ", username=" + username
        + ", email=" + email
        + ", createdAt=" + createdAt
        + "}";
  }
}