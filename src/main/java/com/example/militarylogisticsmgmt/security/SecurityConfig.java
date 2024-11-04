package com.example.militarylogisticsmgmt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Security configuration class.
 *
 * <p>This configuration class sets up web security for the application using Spring Security.
 * It allows customization of the application's security policies and defines which requests
 * are ignored by the security filters.
 * </p>
 *
 * <p>During development, this configuration disables security for all requests,
 * meaning that no requests require authentication or authorization.
 * Security will be added at a later date.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Configures a {@link WebSecurityCustomizer} bean to ignore all requests.
   *
   * <p>This bean sets up the security filter chain to bypass security checks for all incoming
   * requests, effectively disabling security for the application.
   * </p>
   *
   * @return a {@code WebSecurityCustomizer} that ignores all requests
   */
  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().anyRequest();
  }

}
