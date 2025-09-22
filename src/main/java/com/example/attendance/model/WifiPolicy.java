package com.example.attendance.model;

public enum WifiPolicy {
    PUBLIC_IP,           // compare teacher vs student public IP
    NETWORK_SIGNATURE,   // compare custom string set by teacher (e.g., SSID hash)
    BOTH                 // require both to match
}
