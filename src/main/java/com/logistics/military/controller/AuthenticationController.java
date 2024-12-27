package com.logistics.military.controller;

import com.logistics.military.dto.AuthTokensDto;
import com.logistics.military.dto.UserRequestDto;
import com.logistics.military.dto.UserResponseDto;
import com.logistics.military.security.TokenService;
import com.logistics.military.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for user authentication and refresh tokens endpoints.
 *
 * <p>This controller exposes two endpoints:
 * - POST /auth/login: Allows a user to login using their credentials in the form of a
 *   {@link UserRequestDto}.
 * - POST /refresh-token: Refreshes the authentication tokens for a user.
 * </p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final AuthenticationService authenticationService;
  private final TokenService tokenService;

  public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
  public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

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
      Cookie accessTokenCookie =
          new Cookie(ACCESS_TOKEN_COOKIE_NAME, authTokensDto.getAccessToken());
      accessTokenCookie.setHttpOnly(true);
      accessTokenCookie.setSecure(true); // Requires HTTPS in production
      accessTokenCookie.setPath("/");
      accessTokenCookie.setMaxAge(15 * 60); // 15 minutes

      response.addCookie(accessTokenCookie);

      Cookie refreshTokenCookie =
          new Cookie(REFRESH_TOKEN_COOKIE_NAME, authTokensDto.getRefreshToken());
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
          if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
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
      if (decodedJwt.getExpiresAt() == null || decodedJwt.getExpiresAt().isBefore(Instant.now())) {
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
      Cookie newAccessTokenCookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, newAccessToken);
      newAccessTokenCookie.setHttpOnly(true);
      newAccessTokenCookie.setSecure(true); // Requires HTTPS is used in production
      newAccessTokenCookie.setPath("/");
      newAccessTokenCookie.setMaxAge(15 * 60); // 15 minutes expiration

      // Create the refresh token cookie
      Cookie newRefreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, newRefreshToken);
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
