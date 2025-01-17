package com.logistics.military.config;

import com.logistics.military.security.JwtAuthenticationFilter;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration class for setting up web security in the application.
 *
 * <p>This class configures the security settings for the application using Spring Security.
 * It defines beans for password encoding, authentication management, and JWT-based authentication.
 * The security configuration includes rules for authorization, JWT decoding and encoding,
 * and role-based access control for various endpoints in the application.</p>
 *
 * <p>The security configuration ensures that certain endpoints are publicly accessible
 * (e.g., "/auth/**", POST requests to "/users" and "/users/"), while others require specific roles
 * (e.g., "/admin/**" requires the "ADMIN" role). All other requests are secured and require
 * authentication. Security is enforced for the entire application, with access control applied
 * based on user roles.</p>
 *
 * <p>Session management is configured to be stateless, suitable for REST APIs using JWT tokens
 * for authentication. The JWT tokens are signed using an RSA key pair and validated with the
 * public key.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  /**
   * Creates a {@link PasswordEncoder} bean for securely encoding passwords.
   *
   * <p>This method configures a {@link PasswordEncoder} to encode passwords using the
   * {@link BCryptPasswordEncoder} algorithm. BCrypt is a widely used password-hashing algorithm
   * that is resistant to brute-force and rainbow table attacks, ensuring the security of stored
   * passwords.
   * </p>
   *
   * <p>The {@link PasswordEncoder} returned by this method will be used by the application to hash
   * passwords during user registration and authentication processes.</p>
   *
   * @return a {@link PasswordEncoder} that uses {@link BCryptPasswordEncoder} to securely encode
   *     passwords.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Creates an {@link AuthenticationManager} bean for authentication management.
   *
   * <p>This method sets up an {@link AuthenticationManager} with a
   * {@link DaoAuthenticationProvider}, which uses the {@link UserDetailsService} for
   * loading user-specific data and the {@link PasswordEncoder} for encoding passwords during
   * authentication.
   * </p>
   *
   * @param detailsService the user details service used to load user data for authentication
   * @return an {@link AuthenticationManager} for managing authentication
   */
  @Bean
  public AuthenticationManager authManager(UserDetailsService detailsService) {
    DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
    daoProvider.setUserDetailsService(detailsService);
    daoProvider.setPasswordEncoder(passwordEncoder());
    return new ProviderManager(daoProvider);
  }

  /**
   * Configures the security filter chain to define how HTTP requests are authorized.
   *
   * <p>This method sets up rules for authorizing HTTP requests, such as which endpoints are
   * publicly accessible (e.g., "/auth/**", "/users", "/users/"), which require specific roles
   * (e.g., "/admin/**" requires "ADMIN" role), and which require authentication. It also disables
   * CSRF and configures session management to be stateless, since the REST API uses JWT for
   * stateless authentication and CSRF is unnecessary.
   * </p>
   *
   * @param http the {@link HttpSecurity} object used to configure HTTP security settings
   * @return the configured {@link SecurityFilterChain} object
   * @throws Exception if the configuration fails
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable) // CSRF unnecessary for stateless JWT authentication
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers("/auth/**").permitAll();
          auth.requestMatchers(HttpMethod.POST, "/users", "/users/").permitAll();
          auth.requestMatchers("/admin/**").hasRole("ADMIN");
          auth.requestMatchers("/users/**").hasAnyRole("ADMIN", "USER");
          auth.anyRequest().authenticated();
        });
    http
        .oauth2ResourceServer(oauth2ResourceServer ->
            oauth2ResourceServer
                .jwt(jwt ->
                    jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));
    http
        .sessionManagement(session ->
            session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Configures Cross-Origin Resource Sharing (CORS) for the application.
   *
   * <p>This method defines CORS settings to allow requests from the allowed origin defined in
   * a .env file or a system environment variable for CI/CD environments. By configuring CORS
   * this way, the backend RESTful API permits only requests originating from the frontend and
   * the origin can be easily updated for use in development or production.</p>
   *
   * <p>The CORS policy specifies the allowed HTTP methods (<code>GET</code>, <code>POST</code>,
   * <code>PUT</code>, and <code>DELETE</code>), ensuring that the frontend can perform these
   * operations on backend resources. This configuration is essential for enabling secure and
   * controlled communication between the frontend and backend.</p>
   *
   * @return a {@link UrlBasedCorsConfigurationSource} configured with the CORS settings
   */
  @Bean
  UrlBasedCorsConfigurationSource apiCorsConfigurationSource() {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    String allowedOrigin = dotenv.get("FRONTEND_ORIGIN");
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of(allowedOrigin == null ? System.getenv("FRONTEND_ORIGIN") : allowedOrigin));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}