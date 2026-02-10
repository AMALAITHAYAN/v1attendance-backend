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
    CommandLineRunner seedUsers(UserRepository users) {
        return args -> {

            // SUPER ADMIN
            if (users.findByUsername("superadmin@gmail.com").isEmpty()) {
                User superAdmin = new User();
                superAdmin.setUsername("superadmin@gmail.com");
                superAdmin.setPassword("admin123"); // change later
                superAdmin.setRole(UserRole.SUPER_ADMIN);
                users.save(superAdmin);
            }

            // TEACHER
            if (users.findByUsername("meera@college.edu").isEmpty()) {
                User teacher = new User();
                teacher.setUsername("meera@college.edu");
                teacher.setPassword("secret"); // change later
                teacher.setRole(UserRole.TEACHER);
                users.save(teacher);
            }
        };
    }
}
