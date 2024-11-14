package com.logistics.military;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Military Logistics Management System application.
 *
 * <p>This class is the main class for the Spring Boot application.
 * It initializes the Spring application context and launches the application.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 *     Run this class to start the application:
 *     {@code java -jar military-logistics-management-system.jar}
 * </pre>
 *
 */
@SpringBootApplication
public class MilitaryLogisticsManagementSystemApplication {

  /**
   * Main method that serves as the entry point for the application.
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(MilitaryLogisticsManagementSystemApplication.class, args);
  }

}
