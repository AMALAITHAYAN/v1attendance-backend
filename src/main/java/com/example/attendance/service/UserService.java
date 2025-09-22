package com.example.attendance.service;

import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> login(String username, String password) {
        return users.findByUsername(username.toLowerCase())
                .filter(u -> u.getPassword().equals(password));
    }

    public boolean isRole(String username, String password, UserRole role) {
        return login(username, password).map(u -> u.getRole() == role).orElse(false);
    }

    public User createUser(String username, String password, UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setRole(role);
        return users.save(u);
    }

    public boolean exists(String username) {
        return users.existsByUsername(username.toLowerCase());
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = users.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getPassword().equals(oldPassword)) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        user.setPassword(newPassword);
        users.save(user);
    }
}
