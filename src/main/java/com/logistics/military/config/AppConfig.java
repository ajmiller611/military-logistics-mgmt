package com.logistics.military.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for application-wide beans.
 *
 * <p>This class defines shared bean configurations that can be injected
 * into other components within the application context.
 * </p>
 */
@Configuration
public class AppConfig {

  /**
   * Provides a {@link Clock} bean set to the system's default time zone.
   *
   * @return a {@link Clock} instance set to the system's default time zone
   */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
