package com.logistics.military.controller;

import com.logistics.military.service.LogisticsUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling user-related HTTP requests.
 *
 * <p>This controller exposes endpoints for users with the 'user' role access level.
 * It also facilitates interaction with the {@link LogisticsUserService} for user-related
 * operations.
 * </p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class LogisticsUserController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final LogisticsUserService logisticsUserService;

  /**
   * An endpoint to test role based authentication.
   *
   * @return a string indicating access level
   */
  @GetMapping("/")
  public String userRoleAccess() {
    return "User access level";
  }
}
