package org.example.gdelt;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GdeltEventPublisher {
    private static final int MAX_CACHED = 600;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<GdeltEventMarker> cache = new ArrayList<>();

    public synchronized SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));

        emitters.add(emitter);

        try {
            emitter.send(SseEmitter.event().comment("connected"));
            for (GdeltEventMarker marker : cache) {
                emitter.send(SseEmitter.event().name("event").data(marker));
            }
        } catch (IOException ex) {
            emitter.complete();
            emitters.remove(emitter);
        }

        return emitter;
    }

    public synchronized void publish(GdeltEventMarker marker) {
        cache.add(marker);
        if (cache.size() > MAX_CACHED) {
            cache.remove(0);
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("event").data(marker));
            } catch (IOException ex) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
}
