package org.example.gdelt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Spring Boot app that polls GDELT's "lastupdate.txt" endpoint and streams
 * events from each newly published 15-minute events zip file.
 */
@SpringBootApplication
@EnableScheduling
public class GdeltRealtimeConnector {
    private static final URI LAST_UPDATE_URI = URI.create("http://data.gdeltproject.org/gdeltv2/lastupdate.txt");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter GDELT_DATE_ADDED_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss");

    private final HttpClient client;
    private final Set<String> processedFiles;
    private final GdeltEventPublisher eventPublisher;

    @Value("${gdelt.poll-interval-seconds:60}")
    private long pollIntervalSeconds;

    public GdeltRealtimeConnector(GdeltEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        this.processedFiles = ConcurrentHashMap.newKeySet();
    }

    public static void main(String[] args) {
        SpringApplication.run(GdeltRealtimeConnector.class, args);
    }

    @Scheduled(initialDelayString = "1000", fixedDelayString = "#{${gdelt.poll-interval-seconds:60} * 1000}")
    public void scheduledPoll() {
        safePoll();
    }

    private void safePoll() {
        try {
            pollOnce();
        } catch (Exception e) {
            System.err.printf(Locale.ROOT, "Poll failed: %s%n", e.getMessage());
        }
    }

    void pollOnce() throws IOException, InterruptedException {
        String zipPath = fetchLatestExportZipPath();
        if (zipPath == null) {
            return;
        }

        if (!processedFiles.add(zipPath)) {
            return;
        }

        URI zipUri = URI.create(zipPath);
        System.out.printf(Locale.ROOT,
                "[%s] New event batch (poll every %ds): %s%n",
                Instant.now(),
                pollIntervalSeconds,
                zipUri);

        List<GdeltEvent> events = downloadAndParseEvents(zipUri);
        printEvents(events);
        publishEvents(events);
    }

    private String fetchLatestExportZipPath() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(LAST_UPDATE_URI)
                .GET()
                .timeout(HTTP_TIMEOUT)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("lastupdate fetch failed with HTTP " + response.statusCode());
        }

        return Arrays.stream(response.body().split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(this::extractPath)
                .filter(path -> path != null && path.endsWith(".export.CSV.zip"))
                .findFirst()
                .orElseThrow(() -> new IOException("No events export path found in lastupdate.txt"));
    }

    private String extractPath(String line) {
        String[] tokens = line.split("\\s+");
        if (tokens.length < 3) {
            return null;
        }
        return tokens[2].trim();
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
                    "id=%s time=%s actor1=%s actor2=%s code=%s tone=%s lat=%s lon=%s url=%s%n",
                    event.globalEventId(),
                    formatEventTimestampUtc(event),
                    sanitize(event.actor1Name()),
                    sanitize(event.actor2Name()),
                    event.eventCode(),
                    event.avgTone(),
                    event.latitude(),
                    event.longitude(),
                    sanitize(event.sourceUrl()));
        }
    }

    private void publishEvents(List<GdeltEvent> events) {
        for (GdeltEvent event : events) {
            if (event.latitude() == null || event.longitude() == null) {
                continue;
            }

            eventPublisher.publish(new GdeltEventMarker(
                    event.globalEventId(),
                    formatEventTimestampUtc(event),
                    sanitize(event.actor1Name()),
                    sanitize(event.actor2Name()),
                    event.eventCode(),
                    event.avgTone(),
                    event.numMentions(),
                    event.numSources(),
                    event.latitude(),
                    event.longitude(),
                    sanitize(event.sourceUrl())
            ));
        }
    }

    private static String formatEventTimestampUtc(GdeltEvent event) {
        if (event.dateAdded() == null || event.dateAdded().isBlank()) {
            return "-";
        }

        try {
            LocalDateTime timestamp = LocalDateTime.parse(event.dateAdded(), GDELT_DATE_ADDED_FORMAT);
            return timestamp.atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_FORMAT) + " UTC";
        } catch (DateTimeParseException ex) {
            return "-";
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
