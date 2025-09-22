// src/main/java/com/example/attendance/live/LiveSessionHub.java
package com.example.attendance.live;

import com.example.attendance.model.Session;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@EnableScheduling
public class LiveSessionHub {

    private final Set<SseEmitter> clients = new CopyOnWriteArraySet<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public SseEmitter subscribe() {
        SseEmitter em = new SseEmitter(0L); // no timeout
        clients.add(em);
        em.onCompletion(() -> clients.remove(em));
        em.onTimeout(() -> {
            try { em.complete(); } catch (Exception ignored) {}
            clients.remove(em);
        });
        em.onError((ex) -> clients.remove(em));
        return em;
    }

    /** Fire a “session-started” message with the wording you requested. */
    public void broadcastSessionStarted(Session s) {
        String msg = "Teacher " + s.getTeacher().getName()
                + " started session for " + s.getClassName()
                + " in " + s.getDepartment() + ", " + s.getYear()
                + " at " + s.getStartTime().format(FMT) + ".";
        send("session-started", uniqueId(), "{\"message\":\"" + escape(msg) + "\"}");
    }

    /** Optional: stop message keeps the stream useful. */
    public void broadcastSessionStopped(Session s) {
        String msg = "Teacher " + s.getTeacher().getName()
                + " stopped session for " + s.getClassName()
                + " at " + s.getEndTime().format(FMT) + ".";
        send("session-stopped", uniqueId(), "{\"message\":\"" + escape(msg) + "\"}");
    }

    /** Heartbeat every 15s to keep connections/alives and proxies happy. */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        for (SseEmitter em : clients) {
            try {
                em.send(SseEmitter.event().comment("hb"));
            } catch (IOException ex) {
                try { em.complete(); } catch (Exception ignored) {}
                clients.remove(em);
            }
        }
    }

    /* ---------- internal ---------- */

    private void send(String event, String id, String json) {
        for (SseEmitter em : clients) {
            try {
                em.send(SseEmitter.event().name(event).id(id).data(json));
            } catch (IOException ex) {
                try { em.complete(); } catch (Exception ignored) {}
                clients.remove(em);
            }
        }
    }

    private static String uniqueId() { return String.valueOf(System.nanoTime()); }
    private static String escape(String s) { return s.replace("\"", "\\\""); }
}
