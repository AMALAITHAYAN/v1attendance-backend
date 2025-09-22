package com.example.attendance.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.attendance.model.Session;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class QrTokenService {

    public String currentToken(Session s, long nowMillis) {
        int interval = (s.getQrIntervalSeconds() == null || s.getQrIntervalSeconds() <= 0)
                ? 5
                : s.getQrIntervalSeconds();
        long slot = nowMillis / (interval * 1000L);

        Algorithm algo = Algorithm.HMAC256(s.getQrSecret());
        Instant now = Instant.ofEpochMilli(nowMillis);
        Instant exp = now.plusSeconds(interval + 2); // small grace

        return JWT.create()
                .withIssuer("attendance")
                .withClaim("sid", s.getId())
                .withClaim("slot", slot)
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .sign(algo);
    }

    /** returns slot or throws if invalid */
    public long validateToken(Session s, String token, long nowMillis) {
        Algorithm algo = Algorithm.HMAC256(s.getQrSecret());
        DecodedJWT jwt = JWT.require(algo)
                .withIssuer("attendance")
                .withClaim("sid", s.getId())
                .acceptLeeway(2) // seconds
                .build()
                .verify(token);

        long slot = jwt.getClaim("slot").asLong();

        // additionally enforce near-current slot
        int interval = (s.getQrIntervalSeconds() == null || s.getQrIntervalSeconds() <= 0)
                ? 5 : s.getQrIntervalSeconds();
        long currSlot = nowMillis / (interval * 1000L);
        if (Math.abs(currSlot - slot) > 1) {
            throw new IllegalArgumentException("QR token expired");
        }
        return slot;
    }
}
