package com.logistics.military.security;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    return web -> web.ignoring().anyRequest(); // Ignores security for all requests
  }

  /**
   * Creates a bean for password encoding using multiple algorithms.
   *
   * <p>This method sets up a {@link PasswordEncoder} that delegates to
   * specific encoders based on the prefix of the password encoding
   * scheme. It initializes encoders for both {@code bcrypt} and
   * {@code argon2} algorithms, allowing for flexible password
   * encoding strategies in the application.
   * </p>
   *
   * @return a {@link PasswordEncoder} that delegates to the appropriate
   *         encoder based on the specified encoding scheme. The default
   *         encoder used is {@code bcrypt}.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

    return new DelegatingPasswordEncoder("bcrypt", encoders);
  }
}