package com.example.attendance.service;

import com.example.attendance.model.Session;
import com.example.attendance.model.WifiPolicy;
import org.springframework.stereotype.Service;

@Service
public class WifiMatchService {

    public boolean matches(Session s, String studentPublicIp, String studentNetworkSignature) {
        WifiPolicy policy = s.getWifiPolicy() == null ? WifiPolicy.PUBLIC_IP : s.getWifiPolicy();

        boolean ipOk = safeEq(s.getTeacherPublicIp(), studentPublicIp);
        boolean sigOk = safeEq(nonNull(s.getNetworkSignature()), nonNull(studentNetworkSignature));

        return switch (policy) {
            case PUBLIC_IP -> ipOk;
            case NETWORK_SIGNATURE -> sigOk;
            case BOTH -> ipOk && sigOk;
        };
    }

    private boolean safeEq(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }
    private String nonNull(String s) { return s == null ? "" : s; }
}
