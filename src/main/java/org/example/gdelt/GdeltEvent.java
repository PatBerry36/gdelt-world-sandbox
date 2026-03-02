package org.example.gdelt;

/**
 * Minimal representation of one row from GDELT 2.0 events export.
 */
public record GdeltEvent(
        String globalEventId,
        String sqlDate,
        String dateAdded,
        String actor1Name,
        String actor2Name,
        String eventCode,
        String avgTone,
        int numMentions,
        int numSources,
        Double latitude,
        Double longitude,
        String sourceUrl
) {
}
