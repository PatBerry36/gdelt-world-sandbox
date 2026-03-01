# GDELT Near-Realtime Java Connector (Spring Boot)

This Spring Boot + Maven project polls GDELT's 3.0 `lastupdate.txt` feed and downloads each newly published
`*.export.CSV.zip` batch (published continuously throughout the day). Parsed events are printed to stdout.

## Requirements

- Java 17+
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
```

The connector polls every 60 seconds (1 minute).


## Test

```bash
mvn test
```
