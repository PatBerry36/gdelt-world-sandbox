package org.example.gdelt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class GdeltParser {
    private GdeltParser() {
    }

    /**
     * Parses rows from GDELT 3 "events" feed where fields are tab-delimited.
     *
     * Column positions are taken from GDELT's event schema:
     *  0 = GLOBALEVENTID
     *  1 = SQLDATE
     * 59 = DATEADDED (UTC timestamp in yyyyMMddHHmmss)
     *  6 = Actor1Name
     * 16 = Actor2Name
     * 26 = EventCode
     * 34 = AvgTone
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
                if (record.size() <= 59) {
                    continue;
                }

                events.add(new GdeltEvent(
                        record.get(0),
                        record.get(1),
                        record.get(59),
                        record.get(6),
                        record.get(16),
                        record.get(26),
                        record.get(34),
                        record.get(57)
                ));
            }
        }

        return events;
    }
}
