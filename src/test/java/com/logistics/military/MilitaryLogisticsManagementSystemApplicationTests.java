package com.logistics.military;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.logistics.military.controller.UserController;
import com.logistics.military.repository.UserRepository;
import com.logistics.military.service.UserService;
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

  private final UserController userController;
  private final UserService userService;
  private final UserRepository userRepository;

  /**
   * Constructor for {@link MilitaryLogisticsManagementSystemApplicationTests}.
   *
   * <p>This constructor allows Spring to inject the necessary beans into
   * the test class, ensuring that the bean are available for testing and that all its
   * dependencies are correctly resolved.
   * </p>
   *
   * @param userController the {@link UserController} to be injected, which handles user operations.
   */
  @Autowired
  public MilitaryLogisticsManagementSystemApplicationTests(
      UserController userController,
      UserService userService,
      UserRepository userRepository
  ) {
    this.userController = userController;
    this.userService = userService;
    this.userRepository = userRepository;
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
    assertThat(userController).isNotNull();
    assertThat(userService).isNotNull();
    assertThat(userRepository).isNotNull();
  }
}
