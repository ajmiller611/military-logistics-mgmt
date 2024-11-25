package com.logistics.military.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for setting up Cross-Origin Resource Sharing (CORS) policies
 * for the application. This class implements {@link WebMvcConfigurer} to customize
 * the global configuration of CORS settings for all incoming HTTP requests.
 *
 * <p>For development, the configuration allows requests from the default React frontend development
 * server running on <code>http://localhost:3000</code> and supports <code>GET</code>,
 * <code>POST</code>, <code>PUT</code>, and <code>DELETE</code> HTTP methods. It also allows
 * all headers and enables the sending of credentials (such as authentication cookies).
 * </p>
 *
 * <p>In production, adjust the allowed origins to restrict access to only trusted domains.
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /**
   * Configures CORS mapping for the application.
   *
   * <p>This method allows cross-origin requests from the frontend running at
   * <code>http://localhost:3000</code> and allows the following HTTP methods: <code>GET</code>,
   * <code>POST</code>, <code>PUT</code>, and <code>DELETE</code>. It also allows any headers
   * and permits the inclusion of credentials in the request.
   * </p>
   *
   * @param registry the {@link CorsRegistry} used to configure CORS mappings
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/*")
        .allowedOrigins("http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
