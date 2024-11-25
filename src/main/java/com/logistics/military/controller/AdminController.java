package com.logistics.military.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling admin-related HTTP requests.
 *
 * <p>This controller exposes endpoints for users with the 'admin' role.</p>
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin("*")
public class AdminController {

  /**
   * An endpoint to test role-based authentication and authorization.
   *
   * <p>This endpoint is used to verify that users with the "ADMIN" role can access
   * the resource and receive the correct access-level message.</p>
   *
   * @return a string indicating the access level for the authenticated user.
   */
  @GetMapping("/")
  public String adminRoleAccess() {
    return "Admin access level";
  }
}
