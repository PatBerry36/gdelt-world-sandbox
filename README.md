# GDELT Near-Realtime Globe (Spring Boot)

This Spring Boot + Maven project polls GDELT's 2.0 `lastupdate.txt` feed and downloads each newly published
`*.export.CSV.zip` batch (published about every 15 minutes).

It now includes a browser UI at `/` that renders a **rotatable wireframe globe** with country + continent outlines,
and plots a marker for each incoming event location.

## Requirements

- Java 17+
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
```

Open http://localhost:8080 to view the globe.

By default, the connector polls every 60 seconds.

To set a custom polling interval in seconds (for example, 30 seconds):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--gdelt.poll-interval-seconds=30"
```

## Test

```bash
mvn test
```
