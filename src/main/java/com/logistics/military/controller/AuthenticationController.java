package com.logistics.military.controller;

import com.logistics.military.dto.AuthTokensDto;
import com.logistics.military.dto.LogisticsUserDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller responsible for user authentication and registration endpoints.
 *
 * <p>This controller exposes two endpoints:
 * - POST /auth/register: Allows a new user to register by passing a {@link UserRequestDto}.
 * - POST /auth/login: Allows a user to login using their credentials in the form of a
 *   {@link UserRequestDto}.
 * </p>
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthenticationController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final AuthenticationService authenticationService;
  private final TokenService tokenService;

  /**
   * Registers a new user in the system.
   *
   * <p>This endpoint receives a {@link UserRequestDto} object containing the user's registration
   * details, including username, password, and email. It delegates user creation to the
   * {@link AuthenticationService} and, upon successful registration, returns a response with the
   * user’s information and the location of the new user resource.
   * </p>
   *
   * <p>The response status is {@code 201 Created} with a `Location` header pointing to the endpoint
   * where the new user's details can be retrieved. The response body includes
   * a {@link UserResponseDto} containing the user's ID, username, email, and creation timestamp.
   * </p>
   *
   * @param body the {@link UserRequestDto} containing the user's registration data
   * @return a {@link ResponseEntity} containing the newly created {@link UserResponseDto}
   *     with a `Location` header for the created resource
   */
  @PostMapping("/register")
  public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRequestDto body) {
    logger.info("Endpoint /auth/register received request: {}", body);

    // Delegate user creation to the authentication service
    LogisticsUserDto userDto = authenticationService.registerNewUser(body);

    // Build the URI location of the newly created user resource
    URI location = ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .path("/users/{id}")
        .buildAndExpand(userDto.getUserId())
        .toUri();

    // Map the created user details to the response DTO
    UserResponseDto userResponseDto = new UserResponseDto(
        userDto.getUserId(),
        userDto.getUsername(),
        userDto.getEmail()
    );

    // Return a response entity with 201 Created status and user details
    ResponseEntity<UserResponseDto> response =
        ResponseEntity.created(location).body((userResponseDto));
    logger.info("Endpoint /auth/register response: {}", response);
    return response;
  }

  /**
   * Handles user login authentication.
   *
   * <p>This method accepts a {@link UserRequestDto} containing the user's credentials
   * (username and password) and calls the {@link AuthenticationService} to authenticate the user.
   * If successful, a {@link UserResponseDto} is returned with the user details and a JWT token.
   * </p>
   *
   * @param body the login data transfer object containing the user's username and password
   * @return a {@link UserResponseDto} containing the user details and the generated JWT token
   */
  @PostMapping("/login")
  public ResponseEntity<UserResponseDto> loginUser(
      @RequestBody UserRequestDto body,
      HttpServletResponse response) {

    logger.info("Endpoint /auth/login received request: {}", body);

    AuthTokensDto authTokensDto = authenticationService.loginUser(body);

    if (authTokensDto.getAccessToken() != null && !authTokensDto.getAccessToken().isEmpty())  {
      Cookie accessTokenCookie = new Cookie("access_token", authTokensDto.getAccessToken());
      accessTokenCookie.setHttpOnly(true);
      accessTokenCookie.setSecure(true); // Requires HTTPS in production
      accessTokenCookie.setPath("/");
      accessTokenCookie.setMaxAge(15 * 60); // 15 minutes

      response.addCookie(accessTokenCookie);

      Cookie refreshTokenCookie =
          new Cookie("refresh_token", authTokensDto.getRefreshToken());
      refreshTokenCookie.setHttpOnly(true);
      refreshTokenCookie.setSecure(true); // Requires HTTPS in production
      refreshTokenCookie.setPath("/");
      refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days

      response.addCookie(refreshTokenCookie);

      UserResponseDto userResponseDto = new UserResponseDto(
          authTokensDto.getLogisticsUserDto().getUserId(),
          authTokensDto.getLogisticsUserDto().getUsername(),
          authTokensDto.getLogisticsUserDto().getEmail()
      );

      logger.info("Endpoint /auth/login response: {}", userResponseDto);
      return ResponseEntity.ok(userResponseDto);
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  /**
   * Refreshes the authentication tokens for a user by validating and processing a refresh token.
   *
   * <p>This method extracts the refresh token from the request cookies, verifies its validity, and
   * generates new access and refresh tokens. The new tokens are then added as cookies in the
   * response for the client to use in subsequent requests.</p>
   *
   * <p>If the refresh token is missing, expired, or invalid, an appropriate error response is
   * returned.</p>
   *
   * @param request the {@link HttpServletRequest} object containing the client's request data,
   *                including cookies.
   * @param response the {@link HttpServletResponse} object used to send the response back to the
   *                 client, including setting cookies with the new tokens.
   * @return a {@link ResponseEntity} indicating the result of the operation. A successful operation
   *         returns HTTP 200 with no body, while errors return HTTP 400 (Bad Request) or HTTP 403
   *         (Forbidden) with an error message.
   */
  @PostMapping("/refresh-token")
  public ResponseEntity<String> refreshToken(
      HttpServletRequest request,
      HttpServletResponse response) {

    try {
      String refreshToken = null;
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("refresh_token".equals(cookie.getName())) {
            refreshToken = cookie.getValue();
            break;
          }
        }
      }

      if (refreshToken == null) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Refresh Token is missing");
      }

      // Decode the refresh token to extract claims
      Jwt decodedJwt = tokenService.jwtDecoder().decode(refreshToken);

      // Extract the username or subject from the refresh token
      String username = decodedJwt.getSubject();

      // Check if the refresh token is expired or invalid
      if (decodedJwt.getExpiresAt().isBefore(Instant.now())) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body("Refresh token has expired");
      }

      // Generate a new access token (JWT) for the user
      Authentication auth = new UsernamePasswordAuthenticationToken(
          username,
          null,
          Collections.emptyList()
      );
      String newAccessToken = tokenService.generateAccessToken(auth);
      String newRefreshToken = tokenService.generateRefreshToken(auth);

      // Create the access token cookie
      Cookie newAccessTokenCookie = new Cookie("access_token", newAccessToken);
      newAccessTokenCookie.setHttpOnly(true);
      newAccessTokenCookie.setSecure(true); // Requires HTTPS is used in production
      newAccessTokenCookie.setPath("/");
      newAccessTokenCookie.setMaxAge(15 * 60); // 15 minutes expiration

      // Create the refresh token cookie
      Cookie newRefreshTokenCookie = new Cookie("refresh_token", newRefreshToken);
      newRefreshTokenCookie.setHttpOnly(true);
      newRefreshTokenCookie.setSecure(true); // Requires HTTPS is used in production
      newRefreshTokenCookie.setPath("/");
      newRefreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days expiration

      response.addCookie(newAccessTokenCookie);
      response.addCookie(newRefreshTokenCookie);

      // Return 200 ok
      return ResponseEntity.ok().build();
    } catch (JwtException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
    }
  }
}
