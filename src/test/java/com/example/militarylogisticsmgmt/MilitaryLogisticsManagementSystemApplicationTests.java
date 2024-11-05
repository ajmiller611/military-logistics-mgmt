package com.example.militarylogisticsmgmt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.example.militarylogisticsmgmt.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class for the application.
 *
 * <p>This class is responsible for running basic application context load tests
 * to verify that the application starts up correctly within the test environment.
 * </p>
 *
 * <p>{@code @ActiveProfiles("test")} activates the "test" profile, loading properties from
 * the application-test.properties file.
 * </p>
 *
 */
@SpringBootTest
@ActiveProfiles("test")
class MilitaryLogisticsManagementSystemApplicationTests {

  private final UserController controller;

  /**
   * Constructor for {@link MilitaryLogisticsManagementSystemApplicationTests}.
   *
   * <p>This constructor allows Spring to inject the necessary {@link UserController} bean into
   * the test class, ensuring that the controller is available for testing and that all its
   * dependencies are correctly resolved.
   * </p>
   *
   * @param controller the {@link UserController} to be injected, which handles user operations.
   */
  @Autowired
  public MilitaryLogisticsManagementSystemApplicationTests(UserController controller) {
    this.controller = controller;
  }

  /**
   * Verifies that the Spring application context loads successfully.
   *
   * <p>This test ensures that the application starts up without errors
   * and that the necessary beans are correctly initialized.
   * </p>
   */
  @Test
  void contextLoads() {
    assertThat(controller).isNotNull();
  }
}
