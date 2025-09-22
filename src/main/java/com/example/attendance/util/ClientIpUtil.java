package com.example.attendance.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtil {
    public static String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // first IP in the list
            return xff.split(",")[0].trim();
        }
        String ip = req.getRemoteAddr();
        return ip == null ? "" : ip;
    }
}
