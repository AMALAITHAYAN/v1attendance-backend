package com.example.attendance.live;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveSessionService {
    // sessionId -> subscribers
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long sessionId) {
        // 0L == no server-side timeout; client should reconnect if needed
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onTimeout(() -> remove(sessionId, emitter));
        emitter.onCompletion(() -> remove(sessionId, emitter));
        emitter.onError(t -> remove(sessionId, emitter));

        // Send an initial "ping"
        try { emitter.send(SseEmitter.event().name("ping").data("ok")); } catch (IOException ignored) {}
        return emitter;
    }

    public void broadcast(Long sessionId, String eventName, Object payload) {
        List<SseEmitter> subs = emitters.get(sessionId);
        if (subs == null) return;
        for (SseEmitter em : subs.toArray(new SseEmitter[0])) {
            try { em.send(SseEmitter.event().name(eventName).data(payload)); }
            catch (IOException e) { remove(sessionId, em); }
        }
    }

    private void remove(Long sessionId, SseEmitter em) {
        List<SseEmitter> subs = emitters.get(sessionId);
        if (subs != null) subs.remove(em);
    }
}
