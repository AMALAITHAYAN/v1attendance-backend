package com.example.attendance.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveStreamService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> bySession = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long sessionId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        bySession.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(sessionId, emitter));
        emitter.onTimeout(() -> remove(sessionId, emitter));
        // small welcome ping
        send(sessionId, "ping", Map.of("ts", System.currentTimeMillis()));
        return emitter;
    }

    public void send(Long sessionId, String event, Object data) {
        List<SseEmitter> list = bySession.get(sessionId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name(event).data(data));
            } catch (IOException ex) {
                dead.add(e);
            }
        }
        if (!dead.isEmpty()) list.removeAll(dead);
    }

    private void remove(Long sessionId, SseEmitter e) {
        List<SseEmitter> list = bySession.get(sessionId);
        if (list != null) list.remove(e);
    }
}
