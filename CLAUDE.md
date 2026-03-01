# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=GdeltParserTest

# Build without running
mvn package
```

## Architecture

This is a Spring Boot app that polls the GDELT 3 near-realtime feed and prints parsed events to stdout. All source files are under `src/main/java/org/example/gdelt/`.

**Data flow:**
1. `GdeltRealtimeConnector` (entry point + scheduler) — polls `http://data.gdeltproject.org/gdeltv3/lastupdate.txt` once per minute. It parses the text file to extract the latest `.export.CSV.zip` URL (column 3), deduplicates via an in-memory `Set<String>`, downloads the zip, and streams its contents to the parser.
2. `GdeltParser` — stateless utility that reads a tab-delimited GDELT events CSV from a `Reader` using Apache Commons CSV. Column indices are hardcoded per the GDELT event schema (0=GLOBALEVENTID, 1=SQLDATE, 6=Actor1Name, 16=Actor2Name, 26=EventCode, 34=AvgTone, 57=SOURCEURL).
3. `GdeltEvent` — a Java record holding the seven extracted fields (all as `String`).

**Key constraint:** Deduplication is in-memory only — `processedFiles` is not persisted, so previously seen batches will be re-processed on restart.
