# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (open http://localhost:8080 for the globe UI)
mvn spring-boot:run

# Run with a custom polling interval (e.g. 30 seconds)
mvn spring-boot:run -Dspring-boot.run.arguments="--gdelt.poll-interval-seconds=30"

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=GdeltParserTest

# Build without running
mvn package
```

## Architecture

A Spring Boot app with two output surfaces: stdout (event log) and a browser UI at `http://localhost:8080`.

**Backend data flow:**
1. `GdeltRealtimeConnector` (entry point + scheduler) — polls `http://data.gdeltproject.org/gdeltv2/lastupdate.txt` on a fixed delay (default 60s). Extracts the full URL of the latest `.export.CSV.zip` from column 3, deduplicates via an in-memory `Set<String>`, downloads and unzips it, then calls `GdeltParser` and passes results to both `printEvents` (stdout) and `publishEvents` (SSE).
2. `GdeltParser` — stateless utility parsing tab-delimited GDELT 2.0 CSV. Hardcoded column indices: 0=GLOBALEVENTID, 1=SQLDATE, 6=Actor1Name, 16=Actor2Name, 26=EventCode, 34=AvgTone, 53=ActionGeo_Lat, 54=ActionGeo_Long, 56=DATEADDED, 57=SOURCEURL. Events missing lat/lon are parsed but excluded from the globe.
3. `GdeltEvent` — record holding all parsed fields; lat/lon are `Double` (nullable).
4. `GdeltEventMarker` — slimmer record for SSE delivery (lat/lon non-null `double`, timestamp pre-formatted as UTC string).

**Frontend / SSE flow:**
- `GdeltEventPublisher` — holds a `CopyOnWriteArrayList` of `SseEmitter`s; `publish()` fans out each `GdeltEventMarker` as a named `"event"` SSE message serialised as JSON.
- `GdeltEventStreamController` — exposes `GET /api/events/stream` (`text/event-stream`).
- `src/main/resources/static/index.html` — single-page app using [globe.gl](https://globe.gl/) (Three.js). Connects to the SSE endpoint, plots orange point markers and ripple rings on a rotatable wireframe globe. Country outlines from `world-atlas` TopoJSON; continent outlines from a GeoJSON dataset. Keeps at most 600 markers and 40 sidebar cards.

**Key constraint:** `processedFiles` deduplication is in-memory only — lost on restart, so the first batch after startup is always processed.

**HTTP note:** GDELT's server presents a `*.storage.googleapis.com` TLS cert that doesn't cover `data.gdeltproject.org`, so all requests use `http://` not `https://`.
