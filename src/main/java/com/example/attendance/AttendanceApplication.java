package com.example.attendance;

import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AttendanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AttendanceApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdmin(UserRepository users) {
		return args -> {
			// Hardcoded Super Admin (from your Phase 1 spec) :contentReference[oaicite:1]{index=1}
			if (users.findByUsername("superadmin@gmail.com").isEmpty()) {
				User admin = new User();
				admin.setUsername("superadmin@gmail.com");   // Gmail/User ID not required for super admin
				admin.setPassword("admin123");     // change in prod
				admin.setRole(UserRole.SUPER_ADMIN);
				users.save(admin);
			}
		};
	}
}
