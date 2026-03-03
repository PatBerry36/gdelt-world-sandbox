package org.example.gdelt;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GdeltParserTest {

    @Test
    void parsesExpectedColumns() throws Exception {
        String[] cols = new String[61];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = "c" + i;
        }

        cols[0] = "123";
        cols[1] = "20260101";
        cols[6] = "Actor One";
        cols[16] = "Actor Two";
        cols[26] = "042";
        cols[31] = "42";
        cols[32] = "7";
        cols[34] = "1.23";
        cols[56] = "38.9072";
        cols[57] = "-77.0369";
        cols[59] = "20260101123045";
        cols[60] = "https://example.com/story";

        String line = String.join("\t", cols);

        List<GdeltEvent> events = GdeltParser.parseEvents(new StringReader(line));

        assertEquals(1, events.size());
        GdeltEvent event = events.get(0);
        assertEquals("123", event.globalEventId());
        assertEquals("20260101", event.sqlDate());
        assertEquals("20260101123045", event.dateAdded());
        assertEquals("Actor One", event.actor1Name());
        assertEquals("Actor Two", event.actor2Name());
        assertEquals("042", event.eventCode());
        assertEquals("1.23", event.avgTone());
        assertEquals(42, event.numMentions());
        assertEquals(7, event.numSources());
        assertEquals(38.9072, event.latitude(), 0.0001);
        assertEquals(-77.0369, event.longitude(), 0.0001);
        assertEquals("https://example.com/story", event.sourceUrl());
    }
}
