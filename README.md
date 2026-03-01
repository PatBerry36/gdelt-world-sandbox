# GDELT Near-Realtime Java Connector (Spring Boot)

This Spring Boot + Maven project polls GDELT's 2.0 `lastupdate.txt` feed and downloads each newly published
`*.export.CSV.zip` batch (published about every 15 minutes). Parsed events are printed to stdout.

## Requirements

- Java 17+
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
```

By default, the connector polls every 60 seconds.

To set a custom polling interval in seconds (for example, 30 seconds):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--gdelt.poll-interval-seconds=30"
```

## Test

```bash
mvn test
```
