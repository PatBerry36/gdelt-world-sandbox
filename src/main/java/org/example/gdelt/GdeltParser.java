package org.example.gdelt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class GdeltParser {
    private static final int MIN_COLUMNS = 58;

    private GdeltParser() {
    }

    /**
     * Parses rows from GDELT 2.0 "events" feed where fields are tab-delimited.
     *
     * Column positions are taken from GDELT's event schema:
     *  0 = GLOBALEVENTID
     *  1 = SQLDATE
     * 53 = ActionGeo_Lat
     * 54 = ActionGeo_Long
     * 56 = DATEADDED (UTC timestamp in yyyyMMddHHmmss)
     * 57 = SOURCEURL
     */
    public static List<GdeltEvent> parseEvents(Reader reader) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter('\t')
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<GdeltEvent> events = new ArrayList<>();
        try (CSVParser parser = new CSVParser(reader, format)) {
            for (CSVRecord record : parser) {
                if (record.size() < MIN_COLUMNS) {
                    continue;
                }

                events.add(new GdeltEvent(
                        record.get(0),
                        record.get(1),
                        record.get(56),
                        record.get(6),
                        record.get(16),
                        record.get(26),
                        record.get(34),
                        parseCoordinate(record.get(53)),
                        parseCoordinate(record.get(54)),
                        record.get(57)
                ));
            }
        }

        return events;
    }

    private static Double parseCoordinate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
