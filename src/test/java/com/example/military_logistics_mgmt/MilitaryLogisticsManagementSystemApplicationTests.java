package com.example.military_logistics_mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.military_logistics_mgmt.controller.UserController;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the Military Logistics Management System application.
 */
@SpringBootTest
@ActiveProfiles("test")
class MilitaryLogisticsManagementSystemApplicationTests {

	@Autowired
	private UserController controller;

	@Test
	void contextLoads() {
		assertThat(controller).isNotNull();
	}
}
