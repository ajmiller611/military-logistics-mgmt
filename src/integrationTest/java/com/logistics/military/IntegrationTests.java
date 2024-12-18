package com.logistics.military;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test Class to test connection to the PostgreSQL Database.
 */
@SpringBootTest
@ActiveProfiles("integration")
class IntegrationTests {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void testDatabaseConnection() {
    Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    assertTrue(result != null && result == 1, "Database connection test failed!");
  }
}
