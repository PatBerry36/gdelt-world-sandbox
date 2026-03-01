package org.example.gdelt;

/**
 * Minimal representation of one row from GDELT 3 events export.
 */
public record GdeltEvent(
        String globalEventId,
        String sqlDate,
        String dateAdded,
        String actor1Name,
        String actor2Name,
        String eventCode,
        String avgTone,
        String sourceUrl
) {
}
