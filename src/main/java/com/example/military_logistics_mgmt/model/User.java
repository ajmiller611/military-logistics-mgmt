package com.example.military_logistics_mgmt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a user in the system.
 * This entity is mapped to the 'users' table in the database.
 * It contains fields for user ID, username, password, email, and creation timestamp.
 */
@Data
@AllArgsConstructor
@Entity
@Table(name = "users") // Specify the table name to avoid reserved keyword issues
public class User {

    @Id
    @GeneratedValue( strategy = GenerationType.AUTO)
    private Long userId;
    private String username;

    // Increase length to accommodate encoded passwords, which can be longer than the original input
    @Column(length = 512)
    private String password;

    private String email;
    private LocalDateTime createdAt;

    public User() {
        // Default constructor required by JPA for entity instantiation
    }

    // Override toString to prevent logging sensitive information, such as the password.
    @Override
    public String toString() {
        return "User{"+
                "userId=" + userId +
                ", username=" + username +
                ", email=" + email +
                ", createdAt=" + createdAt +
                "}";
    }
}