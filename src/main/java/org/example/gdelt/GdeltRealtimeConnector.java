package org.example.gdelt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Polls GDELT's "lastupdate.txt" endpoint and streams events from each newly published
 * 15-minute events zip file.
 */
public final class GdeltRealtimeConnector {
    private static final URI LAST_UPDATE_URI = URI.create("https://data.gdeltproject.org/gdeltv2/lastupdate.txt");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;
    private final Set<String> processedFiles;

    private GdeltRealtimeConnector() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        this.processedFiles = ConcurrentHashMap.newKeySet();
    }

    public static void main(String[] args) {
        long pollSeconds = args.length > 0 ? Long.parseLong(args[0]) : 60;
        GdeltRealtimeConnector connector = new GdeltRealtimeConnector();
        connector.start(pollSeconds);
    }

    private void start(long pollSeconds) {
        System.out.printf(Locale.ROOT,
                "Starting GDELT near-realtime poller (interval=%ds).%n", pollSeconds);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::safePoll, 0, pollSeconds, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping connector...");
            scheduler.shutdown();
        }));
    }

    private void safePoll() {
        try {
            pollOnce();
        } catch (Exception e) {
            System.err.printf(Locale.ROOT, "Poll failed: %s%n", e.getMessage());
        }
    }

    void pollOnce() throws IOException, InterruptedException {
        String line = fetchLastUpdateLine();
        String[] tokens = line.split("\\s+");
        if (tokens.length < 3) {
            throw new IOException("Unexpected lastupdate.txt format: " + line);
        }

        String zipPath = tokens[2].trim();
        if (!zipPath.endsWith(".export.CSV.zip")) {
            return;
        }

        if (!processedFiles.add(zipPath)) {
            return;
        }

        URI zipUri = URI.create("https://data.gdeltproject.org" + zipPath);
        System.out.printf(Locale.ROOT, "[%s] New event batch: %s%n", Instant.now(), zipUri);
        List<GdeltEvent> events = downloadAndParseEvents(zipUri);
        printEvents(events);
    }

    private String fetchLastUpdateLine() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(LAST_UPDATE_URI)
                .GET()
                .timeout(HTTP_TIMEOUT)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("lastupdate fetch failed with HTTP " + response.statusCode());
        }

        String first = response.body().lines().findFirst().orElse("").trim();
        if (first.isBlank()) {
            throw new IOException("lastupdate.txt returned empty body");
        }
        return first;
    }

    private List<GdeltEvent> downloadAndParseEvents(URI zipUri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(zipUri)
                .GET()
                .timeout(HTTP_TIMEOUT)
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("events zip fetch failed with HTTP " + response.statusCode());
        }

        try (InputStream body = response.body();
             ZipInputStream zis = new ZipInputStream(body, StandardCharsets.UTF_8)) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("Zip file contained no entries: " + zipUri);
            }

            Reader reader = new InputStreamReader(zis, StandardCharsets.UTF_8);
            return GdeltParser.parseEvents(reader);
        }
    }

    private void printEvents(List<GdeltEvent> events) {
        System.out.printf(Locale.ROOT, "Parsed %,d events.%n", events.size());
        for (GdeltEvent event : events) {
            System.out.printf(Locale.ROOT,
                    "id=%s date=%s actor1=%s actor2=%s code=%s tone=%s url=%s%n",
                    event.globalEventId(),
                    event.sqlDate(),
                    sanitize(event.actor1Name()),
                    sanitize(event.actor2Name()),
                    event.eventCode(),
                    event.avgTone(),
                    sanitize(event.sourceUrl()));
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
