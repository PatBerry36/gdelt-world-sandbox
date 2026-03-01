# GDELT Near-Realtime Java Connector

This Maven project polls GDELT's 2.0 `lastupdate.txt` feed and downloads each newly published
`*.export.CSV.zip` batch (published about every 15 minutes). Parsed events are printed to stdout.

## Requirements

- Java 17+
- Maven 3.9+

## Run

```bash
mvn compile exec:java
```

By default, the connector polls every 60 seconds.

To set a custom polling interval in seconds (for example, 30 seconds):

```bash
mvn compile exec:java -Dexec.args="30"
```

## Test

```bash
mvn test
```
