package com.example.attendance.controller;

import com.example.attendance.service.LiveStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/teacher/sessions")
public class LiveStreamController {

    private final LiveStreamService stream;

    public LiveStreamController(LiveStreamService stream) {
        this.stream = stream;
    }

    // NOTE: EventSource cannot attach custom headers. Keep this endpoint public or
    // protect via some other mechanism if required.
    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable("id") Long sessionId) {
        return stream.subscribe(sessionId);
    }
}
