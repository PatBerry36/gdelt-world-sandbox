package org.example.gdelt;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class GdeltEventStreamController {
    private final GdeltEventPublisher eventPublisher;

    public GdeltEventStreamController(GdeltEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return eventPublisher.register();
    }
}
