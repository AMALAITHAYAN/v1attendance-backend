// src/main/java/com/example/attendance/controller/AdminSessionController.java
package com.example.attendance.controller;

import com.example.attendance.live.LiveSessionHub;
import com.example.attendance.model.UserRole;
import com.example.attendance.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/sessions")

public class AdminSessionController {

    private final UserService users;
    private final LiveSessionHub live;

    public AdminSessionController(UserService users, LiveSessionHub live) {
        this.users = users;
        this.live = live;
    }

    @GetMapping(path = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> live(
            @RequestHeader("X-Auth-Username") String adminUser,
            @RequestHeader("X-Auth-Password") String adminPass
    ) {
        // allow only SUPER_ADMIN
        if (!users.isRole(adminUser, adminPass, UserRole.SUPER_ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        SseEmitter emitter = live.subscribe();

        // Push a first tiny event so the browser marks the stream as “open”
        try {
            emitter.send(SseEmitter.event()
                    .name("event")
                    .data("{\"message\":\"connected\"}")
                    .id(String.valueOf(System.currentTimeMillis())));
        } catch (IOException ignored) {}

        HttpHeaders h = new HttpHeaders();
        h.setCacheControl("no-store");
        h.add("X-Accel-Buffering", "no"); // helpful if there is nginx later

        return ResponseEntity.ok()
                .headers(h)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
